package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TypeGenerator {
    private final TypeResolverFactory typeResolverFactory;

    public String generate(Type type, MapperConfig<?> config) {
        SharedTypeNameIdResolver resolver = typeResolverFactory.createTypeIdResolver(config,
                config.constructType(Object.class));
        return generate(type, resolver);
    }

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
