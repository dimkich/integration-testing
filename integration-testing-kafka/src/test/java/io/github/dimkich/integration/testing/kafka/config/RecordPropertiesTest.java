package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import io.github.sugarcubes.cloner.Cloner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordPropertiesTest {

    private static final Cloner IDENTITY_CLONER = new Cloner() {
        @Override
        public <T> T clone(T object) {
            return object;
        }
    };

    @Test
    void validateOk_onlyType() {
        RecordProperties props = new RecordProperties();
        props.setType("json");

        validatePrepared(props);
    }

    @Test
    void validateOk_onlyBeanRef() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");

        validatePrepared(props);
    }

    @Test
    void validateOk_onlyValue() {
        RecordProperties props = new RecordProperties();
        props.setValue(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_typePlusHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setHeaders(new KafkaHeaderComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_typePlusKey() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setKey(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_fqcnWithoutNested() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");

        validatePrepared(props);
    }

    @Test
    void validateOk_providerNamePlusKeyAndHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setKey(new KafkaComponentProperties());
        props.setHeaders(new KafkaHeaderComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_empty() {
        RecordProperties props = new RecordProperties();

        validatePrepared(props);
    }

    @Test
    void validateFail_beanRefAndType() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setType("json");

        assertThatThrownBy(() -> validatePrepared(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("type");
    }

    @Test
    void validateFail_beanRefWithTargetClass() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setTargetClass(Integer.class);

        assertThatThrownBy(() -> validatePrepared(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass")
                .hasMessageContaining("beanRef");
    }

    @Test
    void validateFail_beanRefWithObjectMapperRef() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setObjectMapperRef("customMapper");

        assertThatThrownBy(() -> validatePrepared(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objectMapperRef")
                .hasMessageContaining("beanRef");
    }

    @Test
    void validateFail_fqcnWithTargetClass() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setTargetClass(Integer.class);

        assertThatThrownBy(() -> validatePrepared(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass")
                .hasMessageContaining("fully qualified class name");
    }

    @Test
    void validateOk_beanRefWithValue() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setValue(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_beanRefWithKey() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setKey(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_beanRefWithHeaders() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setHeaders(new KafkaHeaderComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_typePlusValue() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setValue(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_fqcnWithKey() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setKey(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_fqcnWithHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setHeaders(new KafkaHeaderComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_typePlusValueAndKey() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setValue(new KafkaComponentProperties());
        props.setKey(new KafkaComponentProperties());

        validatePrepared(props);
    }

    @Test
    void validateOk_fqcnWithKeyAndHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setKey(new KafkaComponentProperties());
        props.setHeaders(new KafkaHeaderComponentProperties());

        validatePrepared(props);
    }

    @Test
    void prepare_mergesRecordBaseIntoComponentsAndKeepsComponentOverride() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setTargetClass(Integer.class);
        props.setAddTypeInfoHeaders(true);
        KafkaComponentProperties key = new KafkaComponentProperties();
        key.setType("string");
        props.setKey(key);

        props.prepare(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getValue().getType()).isEqualTo("json");
        assertThat(props.getValue().getTargetClass()).isEqualTo(Integer.class);
        assertThat(props.getHeaders().getType()).isEqualTo("json");
        assertThat(props.getHeaders().getAddTypeInfoHeaders()).isTrue();
        assertThat(props.getKey().getType()).isEqualTo("string");
        assertThat(props.getKey().getTargetClass()).isEqualTo(Integer.class);
    }

    @Test
    void prepare_doesNotMixExclusiveSources() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        KafkaComponentProperties value = new KafkaComponentProperties();
        value.setType("json");
        props.setValue(value);

        props.prepare(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getValue().getType()).isEqualTo("json");
        assertThat(props.getValue().getBeanRef()).isNull();
    }

    @Test
    void prepare_doesNotFillComponentsWithoutSource() {
        RecordProperties props = new RecordProperties();

        props.prepare(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getKey().getType()).isNull();
        assertThat(props.getValue().getType()).isNull();
        assertThat(props.getHeaders().getType()).isNull();
    }

    @Test
    void applyDefaultSource_fillsComponentsWithoutSource() {
        RecordProperties props = new RecordProperties();

        props.applyDefaultSource(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getType()).isEqualTo("string");
        assertThat(props.getKey().getType()).isEqualTo("string");
        assertThat(props.getValue().getType()).isEqualTo("string");
        assertThat(props.getHeaders().getType()).isEqualTo("string");
    }

    @Test
    void applyDefaultSource_keepsExplicitSources() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        KafkaComponentProperties key = new KafkaComponentProperties();
        key.setBeanRef("mySerializer");
        props.setKey(key);

        props.applyDefaultSource(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getType()).isEqualTo("json");
        assertThat(props.getKey().getType()).isNull();
        assertThat(props.getKey().getBeanRef()).isEqualTo("mySerializer");
        assertThat(props.getValue().getType()).isEqualTo("json");
        assertThat(props.getHeaders().getType()).isEqualTo("json");
    }

    @Test
    void applyDefaultSource_wrapsEnvelopeOnlyComponentWithStringDefault() {
        RecordProperties props = new RecordProperties();
        KafkaComponentProperties value = new KafkaComponentProperties();
        value.setBinaryEnvelope("{VER(1)}{CONTENT}");
        props.setValue(value);

        props.applyDefaultSource(new PropertyInheritanceMerger(IDENTITY_CLONER));

        assertThat(props.getValue().getType()).isEqualTo("string");
        assertThat(props.getValue().getBinaryEnvelope()).isEqualTo("{VER(1)}{CONTENT}");
    }

    @Test
    void componentValidateOk_typeAndEnvelope() {
        KafkaComponentProperties component = new KafkaComponentProperties();
        component.setType("json");
        component.setBinaryEnvelope("{VER(1)}{CONTENT}");

        validate(component);
    }

    @Test
    void componentValidateWithoutSourceFail_envelopeWithoutSource() {
        KafkaComponentProperties component = new KafkaComponentProperties();
        component.setBinaryEnvelope("{VER(1)}{CONTENT}");

        assertThatThrownBy(() -> validate(component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binaryEnvelope");
    }

    @Test
    void componentValidateFail_beanRefAndType() {
        KafkaComponentProperties component = new KafkaComponentProperties();
        component.setBeanRef("mySerializer");
        component.setType("json");

        assertThatThrownBy(() -> validate(component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("type");
    }

    @Test
    void headerValidateOk_onlyType() {
        KafkaHeaderComponentProperties headers = new KafkaHeaderComponentProperties();
        headers.setType("string");

        validate(headers);
    }

    @Test
    void headerValidateFail_fqcnWithTargetClass() {
        KafkaHeaderComponentProperties headers = new KafkaHeaderComponentProperties();
        headers.setType("com.example.MySerializer");
        headers.setTargetClass(Integer.class);

        assertThatThrownBy(() -> validate(headers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass")
                .hasMessageContaining("fully qualified class name");
    }

    private static void validatePrepared(RecordProperties props) {
        props.prepare(new PropertyInheritanceMerger(IDENTITY_CLONER));
        props.validate();
    }

    private static void validate(TestSerdeProperties props) {
        SerdeRoleCollector collector = new SerdeRoleCollector();
        props.reportRoles(collector);
        collector.validate();
    }
}
