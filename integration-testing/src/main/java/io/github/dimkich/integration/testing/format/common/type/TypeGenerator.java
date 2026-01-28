package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates string representations of {@link Type} instances used in integration tests.
 * <p>
 * This generator is aware of arrays, parameterized types, generic arrays and wildcard
 * bounds and delegates resolution of logical type ids to {@link SharedTypeNameIdResolver}.
 */
@RequiredArgsConstructor
public class TypeGenerator {
    private final TypeResolverFactory typeResolverFactory;

    /**
     * Generate a string representation for the given {@link Type}.
     * <p>
     * If the resolved class has a logical type id registered in {@link SharedTypeNameIdResolver},
     * that id is used; otherwise the fully-qualified class name or generic signature is returned.
     *
     * @param type   the type to represent
     * @param config Jackson mapper configuration used to create the shared type id resolver
     * @return string representation of the provided type
     */
    public String generate(Type type, MapperConfig<?> config) {
        SharedTypeNameIdResolver resolver = typeResolverFactory.createTypeIdResolver(config,
                config.constructType(Object.class));
        return generate(type, resolver);
    }

    /**
     * Generate a string representation for the given {@link Type} using the provided resolver.
     * <p>
     * Handles {@link Class} (including arrays), {@link ParameterizedType},
     * {@link GenericArrayType} and {@link WildcardType} with upper and lower bounds.
     *
     * @param type     the type to represent
     * @param resolver resolver that provides logical type ids for classes when available
     * @return string representation of the provided type
     */
    private String generate(Type type, SharedTypeNameIdResolver resolver) {
        if (type instanceof Class<?> cls) {
            if (cls.getComponentType() != null) {
                return generate(cls.getComponentType(), resolver) + "[]";
            }
            String name = resolver.idFromClassIfExists(cls);
            return name == null ? cls.getName() : name;
        }
        if (type instanceof ParameterizedType pType) {
            String rawName = generate(pType.getRawType(), resolver);
            String args = Stream.of(pType.getActualTypeArguments())
                    .map(t -> generate(t, resolver))
                    .collect(Collectors.joining(", ", "<", ">"));
            return rawName + args;
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return generate(genericArrayType.getGenericComponentType(), resolver) + "[]";
        }
        if (type instanceof WildcardType wType) {
            if (wType.getLowerBounds().length > 0) {
                return "? super " + generate(wType.getLowerBounds()[0], resolver);
            } else if (wType.getUpperBounds().length > 0 && !wType.getUpperBounds()[0].equals(Object.class)) {
                return "? extends " + generate(wType.getUpperBounds()[0], resolver);
            } else {
                return "?";
            }
        }
        return type.getTypeName();
    }
}
