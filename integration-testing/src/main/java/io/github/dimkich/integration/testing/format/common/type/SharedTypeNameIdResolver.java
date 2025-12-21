package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.impl.TypeNameIdResolver;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SharedTypeNameIdResolver extends TypeNameIdResolver {
    public SharedTypeNameIdResolver(
            MapperConfig<?> config,
            JavaType baseType,
            ConcurrentHashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType
    ) {
        super(config, baseType, typeToId, idToType);
    }
}
