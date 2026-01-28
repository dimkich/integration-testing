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

/**
 * Factory responsible for configuring and creating {@link SharedTypeNameIdResolver}
 * instances used by the test infrastructure.
 * <p>
 * It aggregates type information from all available {@link TestSetupModule}s and
 * builds several lookup structures:
 * <ul>
 *     <li>mapping from parent types to their registered subtypes with logical names</li>
 *     <li>reverse mapping from logical name to concrete subtype</li>
 *     <li>base type mappings used for shared ids across type hierarchies</li>
 * </ul>
 * These mappings are then used to resolve logical type ids, check whether a type
 * represents a collection and to construct Jackson {@link com.fasterxml.jackson.databind.jsontype.TypeIdResolver}
 * instances.
 */
@RequiredArgsConstructor
public class TypeResolverFactory {
    private final List<TestSetupModule> modules;

    private final Map<Class<?>, Set<Pair<Class<?>, String>>> parentToSubTypeMap = new HashMap<>();
    private final Map<String, Pair<Class<?>, String>> subTypes = new HashMap<>();
    private final Map<Class<?>, String> baseTypes = new HashMap<>();
    private final Map<JavaType, SharedTypeNameIdResolver> resolverCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, MapperConfig<?>> configCache = new ConcurrentHashMap<>();

    /**
     * Collects type metadata from all {@link TestSetupModule}s and populates the
     * internal lookup structures.
     * <p>
     * The initialization is performed in three passes:
     * <ol>
     *     <li>register all parent types</li>
     *     <li>register aliases for already known subtypes</li>
     *     <li>register base types, named subtypes, unnamed subtypes and custom
     *     equality rules for {@link MockInvoke}</li>
     * </ol>
     */
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

    /**
     * Creates or retrieves a cached {@link SharedTypeNameIdResolver} for the given
     * Jackson configuration and base type.
     *
     * @param config   mapper configuration used to construct {@link JavaType}s
     * @param baseType logical base type for which a resolver is requested
     * @return a configured resolver instance or {@code null} if the base type is
     * not registered as a parent type
     */
    public SharedTypeNameIdResolver createTypeIdResolver(MapperConfig<?> config, JavaType baseType) {
        return getResolver(config, baseType);
    }

    /**
     * Resolves a concrete Java {@link Class} for the given logical type name.
     * <p>
     * If the supplied name matches a configured subtype alias, the corresponding
     * class is returned. Otherwise the name is treated as a fully qualified class
     * name and resolved via {@link Class#forName(String)}.
     *
     * @param type logical type id or fully qualified class name
     * @return resolved {@link Class} instance
     */
    @SneakyThrows
    public Class<?> getType(String type) {
        Pair<Class<?>, String> pair = subTypes.get(type);
        return pair == null ? Class.forName(type) : pair.getKey();
    }

    /**
     * Determines whether the logical type identified by the given name represents
     * some kind of collection (array or {@link Collection}, except {@code byte[]}).
     *
     * @param type logical type id
     * @return {@code true} if the resolved class is a collection-like type,
     * {@code false} otherwise
     */
    public boolean isCollection(String type) {
        Pair<Class<?>, String> pair = subTypes.get(type);
        if (pair != null) {
            Class<?> cls = pair.getKey();
            return (cls.isArray() && cls != byte[].class) || Collection.class.isAssignableFrom(cls);
        }
        return false;
    }

    /**
     * Internal helper that manages configuration-scoped resolver cache and creates
     * {@link SharedTypeNameIdResolver} instances on demand.
     * <p>
     * Cache entries are invalidated whenever a different {@link MapperConfig}
     * instance is observed for the same configuration class to avoid leaking
     * outdated type information.
     */
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

    /**
     * Registers a new parent type and initializes its known subtypes based on the
     * already registered subtype mappings.
     * <p>
     * If the parent itself is a non-abstract, non-interface, non-primitive type
     * and there is no existing subtype with the same simple name, it is also
     * registered as a subtype using its simple name.
     */
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

    /**
     * Registers a concrete subtype using its simple class name as the logical id.
     */
    private void addSubType(Class<?> subType) {
        addSubType(Pair.of(subType, subType.getSimpleName()));
    }

    /**
     * Convenience method that registers multiple subtypes at once.
     */
    private void addSubTypes(Class<?>... subType) {
        Arrays.stream(subType).forEach(this::addSubType);
    }

    /**
     * Registers a concrete subtype with an explicit logical name and propagates it
     * to all compatible parent types.
     *
     * @throws RuntimeException if a subtype with the same logical name is already registered
     */
    private void addSubType(Pair<Class<?>, String> pair) {
        if (subTypes.containsKey(pair.getValue())) {
            throw new RuntimeException(String.format("SubType with name %s already added", pair.getValue()));
        }
        subTypes.put(pair.getValue(), pair);

        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(pair.getKey()))
                .forEach(e -> e.getValue().add(pair));
    }

    /**
     * Adds an additional logical name for an already known subtype and associates
     * it with all compatible parent types.
     */
    private void addAlias(Class<?> subType, String alias) {
        parentToSubTypeMap.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(subType))
                .forEach(e -> e.getValue().add(Pair.of(subType, alias)));
    }

    /**
     * Registers a base type with a shared logical id used for all compatible
     * implementations.
     * <p>
     * If there is no subtype registered under the same logical name yet, the base
     * type itself is also registered as a subtype.
     *
     * @throws RuntimeException if the base type has already been registered
     */
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
