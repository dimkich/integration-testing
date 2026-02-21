package io.github.dimkich.integration.testing.format.common.factory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import lombok.SneakyThrows;

/**
 * Custom {@link BeanSerializerFactory} that applies project-specific serializer fixes.
 *
 * <p>In particular, it wraps delegating serializers with a typed variant and replaces default
 * iterator serialization with a resettable iterator serializer implementation.
 */
public class FixedBeanSerializerFactory extends BeanSerializerFactory {
    /**
     * Creates the serializer factory with the provided Jackson serializer factory configuration.
     *
     * @param config serializer factory configuration
     */
    public FixedBeanSerializerFactory(SerializerFactoryConfig config) {
        super(config);
    }

    /**
     * Creates a serializer for the given type and wraps delegating serializers with
     * {@link TypedStdDelegatingSerializer} to preserve expected typing behavior.
     *
     * @param prov     serializer provider
     * @param origType original type to serialize
     * @return serializer for the given type
     */
    @Override
    @SneakyThrows
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType origType) {
        JsonSerializer<Object> ser = super.createSerializer(prov, origType);
        if (ser instanceof StdDelegatingSerializer delegatingSerializer) {
            return new TypedStdDelegatingSerializer(delegatingSerializer);
        }
        return ser;
    }

    /**
     * Returns this instance when configuration is unchanged, otherwise creates a new configured
     * {@link FixedBeanSerializerFactory}.
     *
     * @param config serializer factory configuration
     * @return serializer factory instance with the given configuration
     */
    @Override
    public SerializerFactory withConfig(SerializerFactoryConfig config) {
        if (_factoryConfig == config) {
            return this;
        }
        return new FixedBeanSerializerFactory(config);
    }

    /**
     * Builds serializer for iterator types using {@link ResettableIteratorSerializer}.
     *
     * @param config       serialization configuration
     * @param type         declared iterator type
     * @param beanDesc     bean description
     * @param staticTyping static typing flag
     * @param valueType    iterator element type
     * @return serializer for resettable iterator handling
     */
    @Override
    protected JsonSerializer<?> buildIteratorSerializer(
            SerializationConfig config, JavaType type, BeanDescription beanDesc, boolean staticTyping, JavaType valueType) {
        return new ResettableIteratorSerializer(valueType, staticTyping, createTypeSerializer(config, valueType));
    }
}