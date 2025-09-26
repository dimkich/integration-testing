package io.github.dimkich.integration.testing.redis.redisson.convert;

import org.redisson.api.RMultimap;
import org.redisson.api.RObject;

import javax.cache.Cache;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class RBridgeFactory {
    @SuppressWarnings("unchecked")
    public static RBridge create(RObject rObject) {
        if (rObject instanceof Map<?, ?> map) {
            return new MapBridge((Map<Object, Object>) map);
        } else if (rObject instanceof Collection<?> collection) {
            return new CollectionBridge((Collection<Object>) collection);
        } else if (rObject instanceof RMultimap<?, ?> multimap) {
            return new MultimapBridge((RMultimap<Object, Object>) multimap);
        } else if (rObject instanceof Cache<?, ?> cache) {
            return new CacheBridge((Cache<Object, Object>) cache);
        } else if (rObject instanceof Lock) {
            return new LockBridge();
        }
        return new CommonBridge(rObject);
    }
}
