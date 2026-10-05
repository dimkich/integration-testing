package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

/**
 * Kafka record-level serde configuration: the base settings for the {@code value}, {@code key}
 * and {@code headers} components.
 * <p>
 * The record-level fields ({@code type}, {@code beanRef}, {@code targetClass},
 * {@code objectMapperRef} and the Spring Kafka type-info settings) are a base for every
 * component: {@link #prepare(PropertyInheritanceMerger)} merges them into the components when
 * the configuration is loaded, and a component overrides the fields it defines itself. There is
 * no record-level converter: the record is assembled strictly from the three components.
 * <p>
 * The {@code headers} component is typed as {@link KafkaHeaderComponentProperties}: a projection
 * of the core source interfaces without the binary envelope, so {@code binary-envelope} cannot be
 * configured for headers.
 */
@Data
public class RecordProperties {

    private static final String DEFAULT_TYPE = "string";

    @PropertyInheritanceExclusive("ref")
    private String beanRef;

    @PropertyInheritanceExclusive("ref")
    private String type;

    private Type targetClass;
    private String objectMapperRef;

    private KafkaComponentProperties key;
    private KafkaComponentProperties value;
    private KafkaHeaderComponentProperties headers;

    private Boolean addTypeInfoHeaders;
    private Boolean useTypeInfoHeaders;
    private String trustedPackages;

    /**
     * Materializes the components and merges the record-level base into each of them; a
     * component overrides the fields it defines. Called once per connection and topic when the
     * configuration is prepared, so converter resolution only assembles the already prepared
     * components. Defaults are not applied here: see {@link #applyDefaultSource}.
     *
     * @param merger the inheritance merger used for the whole configuration
     */
    public void prepare(PropertyInheritanceMerger merger) {
        if (value == null) {
            value = new KafkaComponentProperties();
        }
        merger.merge(value, this);
        if (key == null) {
            key = new KafkaComponentProperties();
        }
        merger.merge(key, this);
        if (headers == null) {
            headers = new KafkaHeaderComponentProperties();
        }
        merger.merge(headers, this);
    }

    /**
     * Applies the default {@code string} source to a final (terminal) configuration and merges
     * it into the components: a component without its own source gets the default, an explicit
     * source wins. Must only be called after the whole inheritance chain has been merged —
     * applying defaults earlier would materialize them on parent levels and block record-level
     * sources of child topics by the exclusive-group rule.
     *
     * @param merger the inheritance merger used for the whole configuration
     */
    public void applyDefaultSource(PropertyInheritanceMerger merger) {
        if (!StringUtils.hasText(beanRef) && !StringUtils.hasText(type)) {
            type = DEFAULT_TYPE;
        }
        prepare(merger);
    }

    /**
     * Validates the nested components: only they take part in converter creation, so a valid
     * component set means a valid record. The record-level fields are a base merged into the
     * components; a conflict inside the base surfaces through the components that inherit it.
     */
    public void validate() {
        validateLevel(getKey());
        validateLevel(getValue());
        validateLevel(getHeaders());
    }

    private static void validateLevel(TestSerdeProperties props) {
        if (props == null) {
            return;
        }
        SerdeRoleCollector collector = new SerdeRoleCollector();
        props.reportRoles(collector);
        collector.validate();
    }
}
