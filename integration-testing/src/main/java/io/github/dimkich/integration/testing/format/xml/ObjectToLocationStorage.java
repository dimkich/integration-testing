package io.github.dimkich.integration.testing.format.xml;

import io.github.dimkich.integration.testing.Test;

import javax.xml.stream.Location;
import java.util.HashMap;
import java.util.Map;

public class ObjectToLocationStorage {
    private final Map<Object, Location> map = new HashMap<>();
    private boolean collect;

    public void put(Object object, Location location) {
        if (collect && object instanceof Test) {
            map.put(object, location);
        }
    }

    public void start() {
        collect = true;
    }

    public void end() {
        collect = false;
    }

    public Location getLocation(Object object) {
        return map.get(object);
    }
}
