package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TypeResolverFactory {
    private final List<TestSetupModule> modules;

    private final Map<Class<?>, Set<NamedType>> parentToSubTypeMap = new HashMap<>();
    private final Map<String, NamedType> subTypes = new HashMap<>();
    private final Map<JavaType, TypeIdResolver> resolverCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, MapperConfig<?>> configCache = new ConcurrentHashMap<>();


    @PostConstruct
    void init() {
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

    public TypeIdResolver createTypeIdResolver(MapperConfig<?> config, JavaType baseType) {
        return getResolver(config, baseType);
    }

    public boolean isCollection(String type) {
        NamedType namedType = subTypes.get(type);
        if (namedType != null) {
            Class<?> cls = namedType.getType();
            return (cls.isArray() && cls != byte[].class) || Collection.class.isAssignableFrom(cls);
        }
        return false;
    }

    private TypeIdResolver getResolver(MapperConfig<?> config, JavaType baseType) {
        if (!configCache.containsKey(config.getClass())) {
            configCache.put(config.getClass(), config);
        } else if (configCache.get(config.getClass()) != config) {
            configCache.clear();
            configCache.put(config.getClass(), config);
            resolverCache.clear();
        }
        return resolverCache.computeIfAbsent(baseType, bt -> {
            Set<NamedType> subtypes = parentToSubTypeMap.get(bt.getRawClass());
            if (subtypes == null) {
                return null;
            }
            ConcurrentHashMap<String, String> typeToId = new ConcurrentHashMap<>();
            HashMap<String, JavaType> idToType = new HashMap<>();

            for (NamedType t : subtypes) {
                idToType.put(t.getName(), config.constructType(t.getType()));
                typeToId.put(t.getType().getName(), t.getName());
            }
            return new SharedTypeNameIdResolver(config, bt, typeToId, idToType);
        });
    }

    private void addParentType(Class<?> parent) {
        parentToSubTypeMap.computeIfAbsent(parent, p -> subTypes.values().stream()
                .filter(n -> p.isAssignableFrom(n.getType()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void addSubType(Class<?> subType) {
        addSubType(subType, subType.getSimpleName());
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
