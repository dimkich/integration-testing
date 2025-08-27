package io.github.dimkich.integration.testing.format.common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component("testTypeResolverBuilder")
@RequiredArgsConstructor
public class TestTypeResolverBuilder extends StdTypeResolverBuilder {
    private final List<TestSetupModule> modules;

    protected final Map<Class<?>, Set<NamedType>> parentToSubTypeMap = new HashMap<>();
    protected final Map<String, NamedType> subTypes = new HashMap<>();
    @Getter
    private String unwrappedTypeProperty;

    @PostConstruct
    void init() {
        init(JsonTypeInfo.Id.NAME, null);
        inclusion(JsonTypeInfo.As.PROPERTY);
        typeProperty("type");
        unwrappedTypeProperty = "utype";
        for (TestSetupModule module : modules) {
            module.getParentTypes().forEach(this::addParentType);
        }
        for (TestSetupModule module : modules) {
            module.getAliases().forEach(p -> this.addAlias(p.getKey(), p.getValue()));
        }
        for (TestSetupModule module : modules) {
            module.getSubTypesWithName().forEach(p -> this.addSubType(p.getKey(), p.getValue()));
            module.getSubTypes().forEach(this::addSubTypes);
            module.getEqualsMap().forEach(MockInvoke::addEqualsForType);
        }
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
        Set<NamedType> namedTypes = parentToSubTypeMap.get(baseType.getRawClass());
        if (namedTypes == null) {
            return null;
        }
        return super.buildTypeSerializer(config, baseType, namedTypes);
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        Set<NamedType> namedTypes = parentToSubTypeMap.get(baseType.getRawClass());
        if (namedTypes == null) {
            return null;
        }
        return super.buildTypeDeserializer(config, baseType, namedTypes);
    }

    private void addParentType(Class<?> parent) {
        parentToSubTypeMap.computeIfAbsent(parent, p -> subTypes.values().stream()
                .filter(n -> p.isAssignableFrom(n.getType()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void addSubType(Class<?> subType) {
        addSubType(subType, subType.getSimpleName().substring(0, 1).toLowerCase() + subType.getSimpleName().substring(1));
    }

    private void addSubTypes(Class<?>... subType) {
        Arrays.stream(subType).forEach(this::addSubType);
    }

    private void addSubType(Class<?> subType, String name) {
        if (subTypes.containsKey(name)) {
            throw new RuntimeException(String.format("SubType with name %s already added", name));
        }
        NamedType namedType = new NamedType(subType, name);
        subTypes.put(name, namedType);

        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(namedType));
    }

    private void addAlias(Class<?> subType, String alias) {
        NamedType namedType = new NamedType(subType, alias);
        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(namedType));
    }
}
