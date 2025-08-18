package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import io.github.dimkich.integration.testing.format.xml.map.TypedStdDelegatingSerializer;
import lombok.SneakyThrows;

public class FixedBeanSerializerFactory extends BeanSerializerFactory {
    public FixedBeanSerializerFactory(SerializerFactoryConfig config) {
        super(config);
    }

    @Override
    @SneakyThrows
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType origType) {
        JsonSerializer<Object> ser = super.createSerializer(prov, origType);
        if (ser instanceof StdDelegatingSerializer delegatingSerializer) {
            return new TypedStdDelegatingSerializer(delegatingSerializer);
        }
        return ser;
    }

    @Override
    public SerializerFactory withConfig(SerializerFactoryConfig config) {
        if (_factoryConfig == config) {
            return this;
        }
        return new FixedBeanSerializerFactory(config);
    }
}