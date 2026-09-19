package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Record-level Kafka serde configuration.
 *
 * <p><b>Roles:</b> the record-level {@code type}/{@code beanRef} selects a
 * provider for the whole record. Nested {@code value}/{@code key}/{@code headers}
 * configure individual components.
 *
 * <p><b>Combinability:</b>
 * <ul>
 *   <li>{@code type} + {@code key}/{@code headers} — allowed when {@code type}
 *       is a provider name (e.g. {@code json}). The record-level provider
 *       assembles the record, and {@code key}/{@code headers} override their
 *       defaults. Rejected when {@code type} is a fully qualified class name
 *       (contains a dot): the class handles the whole record, so nested parts
 *       would be silently ignored.</li>
 *   <li>{@code type} + {@code value} — rejected. The record-level provider
 *       already defines value serialization.</li>
 *   <li>{@code beanRef} + any nested — rejected. {@code beanRef} returns a
 *       ready bean, bypassing the record factory; nested parts would be lost.</li>
 * </ul>
 *
 * <p><b>Spring providers:</b> {@code spring-json} and {@code spring-xml} are
 * available only at record level, because they write type-info into Kafka
 * {@code Headers}. On component level ({@code value}/{@code key}/{@code headers})
 * use {@code json}/{@code xml} instead — same formatting, no type-info headers.
 *
 * <p><b>Inheritance:</b> record-level {@code type}/{@code beanRef} and the three
 * nested parts {@code value}/{@code key}/{@code headers} form one exclusive group
 * ({@code @PropertyInheritanceExclusive("ref")}). Setting any of them on a child
 * level blocks inheritance of the other group members across levels: a child that
 * defines any part neither inherits record-level refs nor the other parts'
 * subtrees from the parent, so the merged config can never hold both record-level
 * and nested style from different levels.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecordProperties extends SerdeProperties {
    @PropertyInheritanceExclusive("ref")
    private SerdeProperties key;

    /**
     * Nested single-value configuration. Mutually exclusive with the record-level
     * {@code beanRef} and with record-level {@code type}; combining them at one
     * level is rejected by {@link #validate()}.
     */
    @PropertyInheritanceExclusive("ref")
    private SerdeProperties value;
    @PropertyInheritanceExclusive("ref")
    private SerdeProperties headers;

    private Boolean addTypeInfoHeaders;
    private Boolean useTypeInfoHeaders;
    private String trustedPackages;

    @Override
    public void validate() {
        super.validate();
        boolean hasRecordLevel = hasBeanRef() || hasType();
        boolean hasNested = getValue() != null || getKey() != null || getHeaders() != null;

        if (!hasRecordLevel || !hasNested) {
            return;
        }

        if (hasBeanRef()) {
            throw new IllegalArgumentException(
                    "Conflicting serde config: record-level 'beanRef' cannot be combined "
                            + "with nested 'value'/'key'/'headers' at the same level. "
                            + "'beanRef' returns a ready bean as-is, bypassing the record factory, "
                            + "so nested parts would be silently ignored. "
                            + "Pick one style: either a single record-level bean, or assemble "
                            + "the record from value/key/headers.");
        }

        if (getValue() != null) {
            throw new IllegalArgumentException(
                    "Conflicting serde config: record-level 'type' and nested 'value' "
                            + "are mutually exclusive. The record-level provider already defines "
                            + "how the value is serialized. Remove 'value', or drop the "
                            + "record-level 'type' and configure 'value' explicitly.");
        }

        if (hasType() && getType().contains(".") && (getKey() != null || getHeaders() != null)) {
            throw new IllegalArgumentException(String.format(
                    "Conflicting serde config: record-level 'type: %s' is a fully qualified class name. "
                            + "The class is created by Spring as-is and handles the whole record; if it "
                            + "already implements KafkaRecordSerializer, nested 'key'/'headers' would be "
                            + "silently ignored (the record factory is bypassed). "
                            + "Use a provider name (e.g. 'json') if you want nested parts to be applied, "
                            + "or move the FQCN to 'value' if it should only serialize the value part.",
                    getType()));
        }
        // type is a provider name and key/headers are set — record factory handles it, OK
    }
}
