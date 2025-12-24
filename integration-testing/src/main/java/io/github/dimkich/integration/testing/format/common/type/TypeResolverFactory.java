package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TypeResolverFactory {
    private final List<TestSetupModule> modules;

    private final Map<Class<?>, Set<Pair<Class<?>, String>>> parentToSubTypeMap = new HashMap<>();
    private final Map<String, Pair<Class<?>, String>> subTypes = new HashMap<>();
    private final Map<Class<?>, String> baseTypes = new HashMap<>();
    private final Map<JavaType, SharedTypeNameIdResolver> resolverCache = new ConcurrentHashMap<>();
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
            module.getBaseTypes().forEach(this::addBaseType);
            module.getSubTypesWithName().forEach(this::addSubType);
            module.getSubTypes().forEach(this::addSubTypes);
            module.getEqualsMap().forEach(MockInvoke::addEqualsForType);
        }
    }

    public SharedTypeNameIdResolver createTypeIdResolver(MapperConfig<?> config, JavaType baseType) {
        return getResolver(config, baseType);
    }

    @SneakyThrows
    public Class<?> getType(String type) {
        Pair<Class<?>, String> pair = subTypes.get(type);
        return pair == null ? Class.forName(type) : pair.getKey();
    }

    public boolean isCollection(String type) {
        Pair<Class<?>, String> pair = subTypes.get(type);
        if (pair != null) {
            Class<?> cls = pair.getKey();
            return (cls.isArray() && cls != byte[].class) || Collection.class.isAssignableFrom(cls);
        }
        return false;
    }

    private SharedTypeNameIdResolver getResolver(MapperConfig<?> config, JavaType baseType) {
        if (!configCache.containsKey(config.getClass())) {
            configCache.put(config.getClass(), config);
        } else if (configCache.get(config.getClass()) != config) {
            configCache.clear();
            configCache.put(config.getClass(), config);
            resolverCache.clear();
        }
        return resolverCache.computeIfAbsent(baseType, bt -> {
            Set<Pair<Class<?>, String>> subtypes = parentToSubTypeMap.get(bt.getRawClass());
            if (subtypes == null) {
                return null;
            }
            ConcurrentHashMap<String, String> typeToId = new ConcurrentHashMap<>();
            HashMap<String, JavaType> idToType = new HashMap<>();

            for (Pair<Class<?>, String> t : subtypes) {
                idToType.put(t.getValue(), config.constructType(t.getKey()));
                typeToId.put(t.getKey().getName(), t.getValue());
            }
            return new SharedTypeNameIdResolver(config, bt, typeToId, idToType, baseTypes);
        });
    }

    private void addParentType(Class<?> parent) {
        parentToSubTypeMap.computeIfAbsent(parent, p -> subTypes.values().stream()
                .filter(n -> p.isAssignableFrom(n.getKey()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        int modifiers = parent.getModifiers();
        if (!subTypes.containsKey(parent.getSimpleName()) && !Modifier.isAbstract(modifiers)
                && !Modifier.isInterface(modifiers) && !parent.isPrimitive()) {
            addSubType(parent);
        }
    }

    private void addSubType(Class<?> subType) {
        addSubType(Pair.of(subType, subType.getSimpleName()));
    }

    private void addSubTypes(Class<?>... subType) {
        Arrays.stream(subType).forEach(this::addSubType);
    }

    private void addSubType(Pair<Class<?>, String> pair) {
        if (subTypes.containsKey(pair.getValue())) {
            throw new RuntimeException(String.format("SubType with name %s already added", pair.getValue()));
        }
        subTypes.put(pair.getValue(), pair);

        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(pair.getKey()))
                .forEach(e -> e.getValue().add(pair));
    }

    private void addAlias(Class<?> subType, String alias) {
        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(Pair.of(subType, alias)));
    }

    private void addBaseType(Pair<Class<?>, String> pair) {
        if (baseTypes.containsKey(pair.getKey())) {
            throw new RuntimeException(String.format("Base type with %s already added", pair.getKey().getName()));
        }
        baseTypes.put(pair.getKey(), pair.getValue());
        if (!subTypes.containsKey(pair.getValue())) {
            addSubType(pair);
        }
    }
}
