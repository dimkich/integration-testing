package io.github.dimkich.integration.testing.redis.spring.ttl;

import io.github.dimkich.integration.testing.NowSetter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class NowSetterRedisNotifier implements NowSetter {
    private final List<TtlEntityService> services = new ArrayList<>();

    @Override
    public void setNow(ZonedDateTime dateTime) {
        for (TtlEntityService service : services) {
            service.setNow(dateTime);
        }
    }

    public void addService(TtlEntityService service) {
        services.add(service);
    }
}
