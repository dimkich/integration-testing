package io.github.dimkich.integration.testing.xml;

import io.github.dimkich.integration.testing.TestCase;

import javax.xml.stream.Location;
import java.util.HashMap;
import java.util.Map;

public class ObjectToLocationStorage {
    private final Map<Object, Location> map = new HashMap<>();
    private boolean collect;

    public void put(Object object, Location location) {
        if (collect && object instanceof TestCase) {
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
