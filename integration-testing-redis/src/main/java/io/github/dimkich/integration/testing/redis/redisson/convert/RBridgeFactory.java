package io.github.dimkich.integration.testing.redis.redisson.convert;

import org.redisson.api.RMultimap;
import org.redisson.api.RObject;

import java.util.Collection;
import java.util.Map;

public class RBridgeFactory {
    @SuppressWarnings("unchecked")
    public static RBridge create(RObject rObject) {
        if (rObject instanceof Map<?, ?> map) {
            return new MapBridge((Map<Object, Object>) map);
        } else if (rObject instanceof Collection<?> collection) {
            return new CollectionBridge((Collection<Object>) collection);
        } else if (rObject instanceof RMultimap<?, ?> multimap) {
            return new MultimapBridge((RMultimap<Object, Object>) multimap);
        }
        return new CommonBridge(rObject);
    }
}
