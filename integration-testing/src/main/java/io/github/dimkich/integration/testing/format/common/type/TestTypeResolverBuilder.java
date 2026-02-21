package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Custom {@link StdTypeResolverBuilder} used in tests to configure Jackson's
 * polymorphic type handling in a centralized way.
 * <p>
 * The builder delegates all decisions about type ids and subtype registration
 * to {@link TypeResolverFactory} and exposes the names of JSON properties that
 * carry type information (regular and "unwrapped" payloads).
 */
@Component("testTypeResolverBuilder")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TestTypeResolverBuilder extends StdTypeResolverBuilder {
    private final TypeResolverFactory typeResolverFactory;

    @Getter
    private String unwrappedTypeProperty;

    /**
     * Creates a copy of an existing builder with the specified default implementation.
     * <p>
     * Used by Jackson when it needs a derived resolver builder instance that preserves
     * the current configuration while changing default implementation settings.
     *
     * @param base                source builder to copy configuration from
     * @param defaultImpl         default fallback type to use when concrete type id is absent
     * @param typeResolverFactory factory responsible for creating type-id resolvers
     */
    protected TestTypeResolverBuilder(TestTypeResolverBuilder base, Class<?> defaultImpl,
                                      TypeResolverFactory typeResolverFactory) {
        super(base, defaultImpl);
        this.typeResolverFactory = typeResolverFactory;
    }

    /**
     * Initializes default type id strategy for tests:
     * <ul>
     *     <li>use {@link JsonTypeInfo.Id#NAME} as type id</li>
     *     <li>include the id as a JSON property</li>
     *     <li>use {@code "type"} as the main type property</li>
     *     <li>use {@code "utype"} for unwrapped payloads</li>
     * </ul>
     */
    @PostConstruct
    void init() {
        init(JsonTypeInfo.Id.NAME, null);
        inclusion(JsonTypeInfo.As.PROPERTY);
        typeProperty("type");
        unwrappedTypeProperty = "utype";
    }

    /**
     * Returns the set of JSON attribute names that may contain type information
     * for objects handled by this builder.
     *
     * @return immutable set containing {@link #getTypeProperty()} and
     * {@link #getUnwrappedTypeProperty()}
     */
    public Set<String> getTypeAttributes() {
        return Set.of(getTypeProperty(), getUnwrappedTypeProperty());
    }

    /**
     * Determines whether the logical type represented by the given id should be
     * treated as a collection (array or {@link java.util.Collection}).
     *
     * @param type logical type id or alias
     * @return {@code true} if the type is configured as a collection, {@code false} otherwise
     */
    public boolean isCollection(String type) {
        return typeResolverFactory.isCollection(type);
    }

    /**
     * Builds a {@link TypeSerializer} that writes the logical type id as a JSON
     * property using a {@link TypeIdResolver} created by {@link TypeResolverFactory}.
     *
     * @return configured {@link AsPropertyTypeSerializer} or {@code null} when
     * no resolver is available for the given base type
     */
    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeIdResolver idResolver = typeResolverFactory.createTypeIdResolver(config, baseType);
        if (idResolver == null) {
            return null;
        }
        return new AsPropertyTypeSerializer(idResolver, null, _typeProperty);
    }

    /**
     * Builds a {@link TypeDeserializer} that reads the logical type id from a
     * JSON property using a {@link TypeIdResolver} created by
     * {@link TypeResolverFactory}.
     *
     * @return configured {@link AsPropertyTypeDeserializer} or {@code null} when
     * no resolver is available for the given base type
     */
    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeIdResolver idResolver = typeResolverFactory.createTypeIdResolver(config, baseType);
        if (idResolver == null) {
            return null;
        }
        return new AsPropertyTypeDeserializer(baseType, idResolver,
                _typeProperty, _typeIdVisible, null, _includeAs,
                _strictTypeIdHandling(config, baseType));
    }

    /**
     * Sets the default implementation type and returns this builder instance.
     *
     * @param defaultImpl fallback concrete class for polymorphic deserialization
     * @return this builder for fluent chaining
     */
    @Override
    public TestTypeResolverBuilder defaultImpl(Class<?> defaultImpl) {
        _defaultImpl = defaultImpl;
        return this;
    }

    /**
     * Returns a builder configured with a new default implementation.
     * <p>
     * If the provided class matches the current one, this instance is reused.
     *
     * @param defaultImpl fallback concrete class for polymorphic deserialization
     * @return this builder or a copied builder with updated default implementation
     */
    @Override
    public TestTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
        if (_defaultImpl == defaultImpl) {
            return this;
        }
        return new TestTypeResolverBuilder(this, _defaultImpl, typeResolverFactory);
    }
}
