package io.github.dimkich.integration.testing.xml.polymorphic;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.dataformat.xml.XmlTypeResolverBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class PolymorphicUnwrappedResolverBuilder extends XmlTypeResolverBuilder {
    private final Map<Class<?>, Set<NamedType>> parentToSubTypeMap = new HashMap<>();
    private final Map<String, NamedType> subTypes = new HashMap<>();
    @Getter
    @Setter
    private String unwrappedTypeProperty;

    @SneakyThrows
    public static PolymorphicUnwrappedResolverBuilder createDefault() {
        PolymorphicUnwrappedResolverBuilder typeResolverBuilder = new PolymorphicUnwrappedResolverBuilder();
        typeResolverBuilder.init(JsonTypeInfo.Id.NAME, null);
        typeResolverBuilder.inclusion(JsonTypeInfo.As.PROPERTY);
        typeResolverBuilder.typeProperty("type");
        typeResolverBuilder.setUnwrappedTypeProperty("utype");
        typeResolverBuilder.addParentType(Object.class).addParentType(Throwable.class)
                .addAlias(Class.forName("java.util.ImmutableCollections$ListN"), "arrayList")
                .addSubTypes(String.class, Character.class, Long.class, Integer.class, Short.class, Byte.class,
                        Double.class, Float.class, BigDecimal.class, BigInteger.class, Boolean.class, ArrayList.class,
                        LinkedHashMap.class, TreeMap.class, LinkedHashSet.class, TreeSet.class, Class.class,
                        LocalTime.class, LocalDate.class, LocalDateTime.class, ZonedDateTime.class);
        return typeResolverBuilder;
    }

    public PolymorphicUnwrappedResolverBuilder addParentType(Class<?> parent) {
        parentToSubTypeMap.computeIfAbsent(parent, p -> subTypes.values().stream()
                .filter(n -> p.isAssignableFrom(n.getType()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return this;
    }

    public PolymorphicUnwrappedResolverBuilder addSubType(Class<?> subType) {
        return addSubType(subType, subType.getSimpleName().substring(0, 1).toLowerCase() + subType.getSimpleName().substring(1));
    }

    public PolymorphicUnwrappedResolverBuilder addSubTypes(Class<?>... subType) {
        Arrays.stream(subType).forEach(this::addSubType);
        return this;
    }

    public PolymorphicUnwrappedResolverBuilder addSubType(Class<?> subType, String name) {
        if (subTypes.containsKey(name)) {
            throw new RuntimeException(String.format("SubType with name %s already added", name));
        }
        NamedType namedType = new NamedType(subType, name);
        subTypes.put(name, namedType);

        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(namedType));
        return this;
    }

    public PolymorphicUnwrappedResolverBuilder addAlias(Class<?> subType, String alias) {
        NamedType namedType = new NamedType(subType, alias);
        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(namedType));
        return this;
    }

    public Set<String> getTypeAttributes() {
        return Set.of(getTypeProperty(), getUnwrappedTypeProperty());
    }

    public boolean isCollection(String type) {
        NamedType namedType = subTypes.get(type);
        if (namedType != null) {
            return Collection.class.isAssignableFrom(namedType.getType());
        }
        return false;
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (parentToSubTypeMap.containsKey(baseType.getRawClass())) {
            TypeSerializer typeSerializer = super.buildTypeSerializer(config, baseType, parentToSubTypeMap.get(baseType.getRawClass()));
            return new PolymorphicAsPropertyTypeSerializer(typeSerializer.getTypeIdResolver(), null,
                    typeSerializer.getPropertyName());
        }
        return null;
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (parentToSubTypeMap.containsKey(baseType.getRawClass())) {
            TypeDeserializer typeDeserializer = super.buildTypeDeserializer(config, baseType, parentToSubTypeMap.get(baseType.getRawClass()));
            return new PolymorphicAsPropertyTypeDeserializer(this, (AsPropertyTypeDeserializer) typeDeserializer, null);
        }
        return null;
    }
}
