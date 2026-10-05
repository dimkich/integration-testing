package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.sugarcubes.cloner.Cloner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisSchemaPropertiesTest {

    private static final Cloner IDENTITY_CLONER = new Cloner() {
        @Override
        public <T> T clone(T object) {
            return object;
        }
    };

    private final PropertyInheritanceMerger merger = new PropertyInheritanceMerger(IDENTITY_CLONER);

    @Test
    void prepareMergesBaseTypeIntoAllComponents() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        schema.setType("json");

        schema.prepare(merger);

        assertThat(schema.getValue().getType()).isEqualTo("json");
        assertThat(schema.getHashKey().getType()).isEqualTo("json");
        assertThat(schema.getHashValue().getType()).isEqualTo("json");
    }

    @Test
    void prepareMergesBaseBeanRefIntoAllComponents() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        schema.setBeanRef("mySerializer");

        schema.prepare(merger);

        assertThat(schema.getValue().getBeanRef()).isEqualTo("mySerializer");
        assertThat(schema.getHashKey().getBeanRef()).isEqualTo("mySerializer");
        assertThat(schema.getHashValue().getBeanRef()).isEqualTo("mySerializer");
    }

    @Test
    void prepareComponentOverridesBase() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        schema.setType("json");
        schema.setValue(component("string"));

        schema.prepare(merger);

        assertThat(schema.getValue().getType()).isEqualTo("string");
        assertThat(schema.getHashKey().getType()).isEqualTo("json");
        assertThat(schema.getHashValue().getType()).isEqualTo("json");
    }

    @Test
    void applyDefaultSourceSetsStringWhenNoSource() {
        RedisSchemaProperties schema = new RedisSchemaProperties();

        schema.applyDefaultSource(merger);

        assertThat(schema.getType()).isEqualTo("string");
        assertThat(schema.getValue().getType()).isEqualTo("string");
        assertThat(schema.getHashKey().getType()).isEqualTo("string");
        assertThat(schema.getHashValue().getType()).isEqualTo("string");
    }

    @Test
    void applyDefaultSourceKeepsBaseSource() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        schema.setBeanRef("mySerializer");

        schema.applyDefaultSource(merger);

        assertThat(schema.getType()).isNull();
        assertThat(schema.getValue().getBeanRef()).isEqualTo("mySerializer");
        assertThat(schema.getHashKey().getBeanRef()).isEqualTo("mySerializer");
        assertThat(schema.getHashValue().getBeanRef()).isEqualTo("mySerializer");
    }

    @Test
    void applyDefaultSourceComponentSourceWins() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        schema.setValue(component("json"));

        schema.applyDefaultSource(merger);

        assertThat(schema.getValue().getType()).isEqualTo("json");
        assertThat(schema.getHashKey().getType()).isEqualTo("string");
        assertThat(schema.getHashValue().getType()).isEqualTo("string");
    }

    @Test
    void applyDefaultSourceKeepsComponentEnvelope() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        StandardSerdeProperties value = new StandardSerdeProperties();
        value.setBinaryEnvelope("{CONTENT}");
        schema.setValue(value);

        schema.applyDefaultSource(merger);

        assertThat(schema.getValue().getType()).isEqualTo("string");
        assertThat(schema.getValue().getBinaryEnvelope()).isEqualTo("{CONTENT}");
    }

    @Test
    void validateOkAfterDefaults() {
        RedisSchemaProperties schema = new RedisSchemaProperties();

        schema.applyDefaultSource(merger);

        schema.validate();
    }

    @Test
    void validateFailsBeanRefAndTypeInComponent() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        StandardSerdeProperties value = new StandardSerdeProperties();
        value.setBeanRef("mySerializer");
        value.setType("json");
        schema.setValue(value);

        schema.applyDefaultSource(merger);

        assertThatThrownBy(schema::validate).hasMessageContaining("Both 'beanRef' and 'type'");
    }

    @Test
    void validateFailsOptionWithClassRef() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        StandardSerdeProperties value = new StandardSerdeProperties();
        value.setType("com.example.MySerde");
        value.setTargetClass(String.class);
        schema.setValue(value);

        schema.applyDefaultSource(merger);

        assertThatThrownBy(schema::validate)
                .hasMessageContaining("has no effect when 'type' is a fully qualified class name");
    }

    @Test
    void inheritanceBaseTypeFlowsToChild() {
        RedisSchemaProperties parent = new RedisSchemaProperties();
        parent.setType("json");
        RedisSchemaProperties child = new RedisSchemaProperties();

        merger.merge(child, parent);
        child.applyDefaultSource(merger);

        assertThat(child.getType()).isEqualTo("json");
        assertThat(child.getValue().getType()).isEqualTo("json");
        assertThat(child.getHashKey().getType()).isEqualTo("json");
    }

    @Test
    void inheritanceChildSourceBlocksParentType() {
        RedisSchemaProperties parent = new RedisSchemaProperties();
        parent.setType("json");
        RedisSchemaProperties child = new RedisSchemaProperties();
        child.setBeanRef("mySerializer");

        merger.merge(child, parent);
        child.applyDefaultSource(merger);

        assertThat(child.getType()).isNull();
        assertThat(child.getValue().getBeanRef()).isEqualTo("mySerializer");
        assertThat(child.getValue().getType()).isNull();
        assertThat(child.getHashKey().getBeanRef()).isEqualTo("mySerializer");
    }

    @Test
    void inheritanceParentTypeDoesNotOverrideChildComponent() {
        RedisSchemaProperties parent = new RedisSchemaProperties();
        parent.setType("json");
        RedisSchemaProperties child = new RedisSchemaProperties();
        child.setValue(component("string"));

        merger.merge(child, parent);
        child.applyDefaultSource(merger);

        assertThat(child.getValue().getType()).isEqualTo("string");
        assertThat(child.getHashKey().getType()).isEqualTo("json");
    }

    @Test
    void initAppliesDefaultsToConnectionSchema() {
        RedisProperties properties = new RedisProperties();
        properties.setMerger(merger);
        RedisProperties.Connection connection = new RedisProperties.Connection();
        RedisSchemaProperties defaultSchema = new RedisSchemaProperties();
        defaultSchema.setType("json");
        defaultSchema.setValue(component("string"));
        connection.setDefaultSchema(defaultSchema);
        properties.setConnections(Map.of("c", connection));

        properties.init();

        assertThat(defaultSchema.getValue().getType()).isEqualTo("string");
        assertThat(defaultSchema.getHashKey().getType()).isEqualTo("json");
        assertThat(defaultSchema.getHashValue().getType()).isEqualTo("json");
    }

    @Test
    void initValidationWrapsDefaultSchemaPath() {
        RedisProperties properties = new RedisProperties();
        properties.setMerger(merger);
        RedisProperties.Connection connection = new RedisProperties.Connection();
        connection.setDefaultSchema(conflictingSchema());
        properties.setConnections(Map.of("c", connection));

        assertThatThrownBy(properties::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid schema config at connection[c].defaultSchema")
                .hasMessageContaining("Both 'beanRef' and 'type'");
    }

    @Test
    void initValidationWrapsPatternSchemaPath() {
        RedisProperties properties = new RedisProperties();
        properties.setMerger(merger);
        RedisProperties.Connection connection = new RedisProperties.Connection();
        connection.setSchemas(Map.of("bad", conflictingSchema()));
        properties.setConnections(Map.of("c", connection));

        assertThatThrownBy(properties::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid schema config at connection[c].schemas[bad]")
                .hasMessageContaining("Both 'beanRef' and 'type'");
    }

    private static RedisSchemaProperties conflictingSchema() {
        RedisSchemaProperties schema = new RedisSchemaProperties();
        StandardSerdeProperties value = new StandardSerdeProperties();
        value.setBeanRef("mySerializer");
        value.setType("json");
        schema.setValue(value);
        return schema;
    }

    private static StandardSerdeProperties component(String type) {
        StandardSerdeProperties props = new StandardSerdeProperties();
        if (type != null) {
            props.setType(type);
        }
        return props;
    }
}
