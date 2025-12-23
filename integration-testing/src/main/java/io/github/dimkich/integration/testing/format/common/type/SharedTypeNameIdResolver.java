package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.impl.TypeNameIdResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedTypeNameIdResolver extends TypeNameIdResolver {
    private final Map<Class<?>, String> baseTypes;
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

    @Override
    protected String idFromClass(Class<?> cls) {
        String name = idFromClassIfExists(cls);
        if (name != null) {
            return name;
        }
        return super.idFromClass(cls);
    }
}
