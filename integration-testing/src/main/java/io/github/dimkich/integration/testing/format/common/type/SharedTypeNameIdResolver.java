package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.impl.TypeNameIdResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extension of Jackson's {@link TypeNameIdResolver} that shares type id mappings
 * between different base types and additionally supports resolving ids for
 * classes by their assignable base type.
 * <p>
 * The resolver is backed by:
 * <ul>
 *     <li>standard {@code typeToId} / {@code idToType} maps managed by the parent
 *     {@link TypeNameIdResolver}</li>
 *     <li>a {@code baseTypes} map, where keys are base classes or interfaces and
 *     values are their logical type ids</li>
 * </ul>
 * <p>
 * This allows a single logical id to be reused for a whole hierarchy of
 * implementation classes (e.g. different collection implementations).
 */
public class SharedTypeNameIdResolver extends TypeNameIdResolver {
    private final Map<Class<?>, String> baseTypes;

    /**
     * Creates a new resolver instance that reuses the standard Jackson
     * {@link TypeNameIdResolver} infrastructure and augments it with shared
     * base-type mappings.
     *
     * @param config    mapper configuration used to construct {@link JavaType}s
     * @param baseType  base type for which this resolver is created
     * @param typeToId  cache map from fully qualified class name to logical type id
     * @param idToType  cache map from logical type id to {@link JavaType}
     * @param baseTypes mapping from base class or interface to a shared type id
     */
    public SharedTypeNameIdResolver(
            MapperConfig<?> config,
            JavaType baseType,
            ConcurrentHashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType,
            Map<Class<?>, String> baseTypes
    ) {
        super(config, baseType, typeToId, idToType);
        this.baseTypes = baseTypes;
    }

    /**
     * Resolves a type id for the given class using only already known and
     * preconfigured mappings.
     * <p>
     * The lookup is performed in two stages:
     * <ol>
     *     <li>Check existing {@code _typeToId} cache (standard Jackson behaviour).</li>
     *     <li>If missing, search {@code baseTypes} for an entry whose key is
     *     assignable from the given class; when found, cache and return its id.</li>
     * </ol>
     *
     * @param cls class to resolve
     * @return resolved type id, or {@code null} if no mapping is available
     */
    public String idFromClassIfExists(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final String key = cls.getName();
        String name = _typeToId.get(key);
        if (name != null) {
            return name;
        }
        for (Map.Entry<Class<?>, String> entry : baseTypes.entrySet()) {
            if (entry.getKey().isAssignableFrom(cls)) {
                _typeToId.put(key, entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Resolves a type id for the given class, first delegating to
     * {@link #idFromClassIfExists(Class)} to honour shared base-type mappings and
     * then falling back to the default {@link TypeNameIdResolver} strategy.
     *
     * @param cls class to resolve
     * @return non-null type id determined either by shared mappings or by the
     * default resolution algorithm
     */
    @Override
    protected String idFromClass(Class<?> cls) {
        String name = idFromClassIfExists(cls);
        if (name != null) {
            return name;
        }
        return super.idFromClass(cls);
    }
}
