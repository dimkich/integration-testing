package io.github.dimkich.integration.testing.format.common;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Converter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Jackson {@link HandlerInstantiator} that creates serializers, deserializers and
 * other handlers as Spring beans, so handlers can use dependency injection.
 */
public class FormatHandlerInstantiator extends HandlerInstantiator {
    private final AutowireCapableBeanFactory beanFactory;

    /**
     * Creates the instantiator.
     *
     * @param beanFactory the bean factory used to create handlers
     */
    public FormatHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public JsonSerializer<?> serializerInstance(SerializationConfig config,
                                                Annotated annotated, Class<?> implClass) {
        return (JsonSerializer<?>) beanFactory.createBean(implClass);
    }

    @Override
    public JsonDeserializer<?> deserializerInstance(DeserializationConfig config,
                                                    Annotated annotated, Class<?> implClass) {
        return (JsonDeserializer<?>) beanFactory.createBean(implClass);
    }

    @Override
    public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
                                                   Annotated annotated, Class<?> implClass) {
        return (KeyDeserializer) beanFactory.createBean(implClass);
    }

    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
                                                              Annotated annotated, Class<?> implClass) {
        return (TypeResolverBuilder<?>) beanFactory.createBean(implClass);
    }

    @Override
    public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                                                 Annotated annotated, Class<?> implClass) {
        return (TypeIdResolver) beanFactory.createBean(implClass);
    }

    @Override
    public ValueInstantiator valueInstantiatorInstance(MapperConfig<?> config,
                                                       Annotated annotated, Class<?> implClass) {
        return (ValueInstantiator) beanFactory.createBean(implClass);
    }

    @Override
    public ObjectIdGenerator<?> objectIdGeneratorInstance(MapperConfig<?> config,
                                                          Annotated annotated, Class<?> implClass) {
        return (ObjectIdGenerator<?>) beanFactory.createBean(implClass);
    }

    @Override
    public ObjectIdResolver resolverIdGeneratorInstance(MapperConfig<?> config,
                                                        Annotated annotated, Class<?> implClass) {
        return (ObjectIdResolver) beanFactory.createBean(implClass);
    }

    @Override
    public PropertyNamingStrategy namingStrategyInstance(MapperConfig<?> config,
                                                         Annotated annotated, Class<?> implClass) {
        return (PropertyNamingStrategy) beanFactory.createBean(implClass);
    }

    @Override
    public Converter<?, ?> converterInstance(MapperConfig<?> config,
                                             Annotated annotated, Class<?> implClass) {
        return (Converter<?, ?>) beanFactory.createBean(implClass);
    }

    @Override
    public VirtualBeanPropertyWriter virtualPropertyWriterInstance(MapperConfig<?> config,
                                                                   Class<?> implClass) {
        return (VirtualBeanPropertyWriter) beanFactory.createBean(implClass);
    }
}
