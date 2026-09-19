package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordPropertiesTest {

    @Test
    void validateOk_onlyType() {
        RecordProperties props = new RecordProperties();
        props.setType("json");

        props.validate();
    }

    @Test
    void validateOk_onlyBeanRef() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");

        props.validate();
    }

    @Test
    void validateOk_onlyValue() {
        RecordProperties props = new RecordProperties();
        props.setValue(new SerdeProperties());

        props.validate();
    }

    @Test
    void validateOk_typePlusHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setHeaders(new SerdeProperties());

        props.validate();
    }

    @Test
    void validateOk_typePlusKey() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setKey(new SerdeProperties());

        props.validate();
    }

    @Test
    void validateOk_fqcnWithoutNested() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");

        props.validate();
    }

    @Test
    void validateOk_providerNamePlusKeyAndHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setKey(new SerdeProperties());
        props.setHeaders(new SerdeProperties());

        props.validate();
    }

    @Test
    void validateOk_empty() {
        RecordProperties props = new RecordProperties();

        props.validate();
    }

    @Test
    void validateFail_beanRefAndType() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setType("json");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("type");
    }

    @Test
    void validateFail_beanRefWithValue() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setValue(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("nested");
    }

    @Test
    void validateFail_beanRefWithKey() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setKey(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("nested");
    }

    @Test
    void validateFail_beanRefWithHeaders() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setHeaders(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beanRef")
                .hasMessageContaining("nested");
    }

    @Test
    void validateFail_typePlusValue() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setValue(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type")
                .hasMessageContaining("value");
    }

    @Test
    void validateFail_fqcnWithKey() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setKey(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("com.example.MySerializer")
                .hasMessageContaining("fully qualified class name");
    }

    @Test
    void validateFail_fqcnWithHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setHeaders(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("com.example.MySerializer")
                .hasMessageContaining("fully qualified class name");
    }

    @Test
    void validateFail_beanRefWithTargetClass() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setTargetClass(Integer.class);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass")
                .hasMessageContaining("beanRef");
    }

    @Test
    void validateFail_beanRefWithObjectMapperRef() {
        RecordProperties props = new RecordProperties();
        props.setBeanRef("mySerializer");
        props.setObjectMapperRef("customMapper");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objectMapperRef")
                .hasMessageContaining("beanRef");
    }

    @Test
    void validateFail_fqcnWithTargetClass() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setTargetClass(Integer.class);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass")
                .hasMessageContaining("fully qualified class name");
    }

    @Test
    void validateFail_typePlusValueAndKey() {
        RecordProperties props = new RecordProperties();
        props.setType("json");
        props.setValue(new SerdeProperties());
        props.setKey(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type")
                .hasMessageContaining("value");
    }

    @Test
    void validateFail_fqcnWithKeyAndHeaders() {
        RecordProperties props = new RecordProperties();
        props.setType("com.example.MySerializer");
        props.setKey(new SerdeProperties());
        props.setHeaders(new SerdeProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("com.example.MySerializer");
    }
}
