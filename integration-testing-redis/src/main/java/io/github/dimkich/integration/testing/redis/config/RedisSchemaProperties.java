package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Redis data-schema configuration: how values, hash field names and hash field values are
 * serialized.
 *
 * <p>A schema configures the individual components ({@link #value}, {@link #hashKey},
 * {@link #hashValue}), each a single-slot source, and the record-level base
 * ({@link #type}, {@link #beanRef}, {@link #targetClass}, {@link #objectMapperRef}). The base
 * is merged into every component by {@link #prepare(PropertyInheritanceMerger)} when the
 * configuration is loaded, and a component overrides the fields it defines itself. There is no
 * schema-level converter: the schema is assembled strictly from the three components.
 *
 * <p>Each unconfigured component inherits from the parent schema by the general
 * property-merge rules (see {@link PropertyInheritanceMerger}): the connection
 * {@link RedisProperties#getDefaultSchema() default schema} inherits the global default schema,
 * and each per-key-pattern schema inherits the connection default schema. Within a component,
 * {@code bean-ref} and {@code type} are exclusive alternatives; {@code binary-envelope} is an
 * ordinary component field that merges together with the inherited source. The
 * {@code string} default applies to a terminal schema via {@link #applyDefaultSource}.
 *
 * @see RedisProperties
 * @see io.github.dimkich.integration.testing.redis.serde.RedisSchemaSerdeFactory
 */
@Data
public class RedisSchemaProperties {

    private static final String DEFAULT_TYPE = "string";

    @PropertyInheritanceExclusive("ref")
    private String beanRef;

    @PropertyInheritanceExclusive("ref")
    private String type;

    private Type targetClass;
    private String objectMapperRef;

    /** Codec for string values and for elements of list, set, zset, and stream. */
    private StandardSerdeProperties value;

    /** Codec for hash field names. */
    private StandardSerdeProperties hashKey;

    /** Codec for hash field values. */
    private StandardSerdeProperties hashValue;

    /** When {@code true}, matching keys are skipped in assertions and replication processing. */
    private boolean ignore;

    /** Dot-separated field paths to exclude for values under this schema. */
    private Set<String> excludedFields;

    /**
     * Materializes the components and merges the record-level base into each of them; a
     * component overrides the fields it defines. Called when the configuration is prepared, so
     * converter resolution only assembles the already prepared components. Defaults are not
     * applied here: see {@link #applyDefaultSource}.
     *
     * @param merger the inheritance merger used for the whole configuration
     */
    public void prepare(PropertyInheritanceMerger merger) {
        if (value == null) {
            value = new StandardSerdeProperties();
        }
        merger.merge(value, this);
        if (hashKey == null) {
            hashKey = new StandardSerdeProperties();
        }
        merger.merge(hashKey, this);
        if (hashValue == null) {
            hashValue = new StandardSerdeProperties();
        }
        merger.merge(hashValue, this);
    }

    /**
     * Applies the default {@code string} source to a final (terminal) schema and merges it into
     * the components: a component without its own source gets the default, an explicit source
     * wins. Must only be called after the whole inheritance chain has been merged — applying
     * defaults earlier would materialize them on parent schemas and block schema-level sources
     * of child schemas by the exclusive-group rule.
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
     * Validates the nested components: only they take part in codec creation, so a valid
     * component set means a valid schema. The record-level fields are a base merged into the
     * components; a conflict inside the base surfaces through the components that inherit it.
     */
    public void validate() {
        validateLevel(getValue());
        validateLevel(getHashKey());
        validateLevel(getHashValue());
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
