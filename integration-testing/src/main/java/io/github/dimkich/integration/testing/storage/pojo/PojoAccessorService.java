package io.github.dimkich.integration.testing.storage.pojo;

import io.github.dimkich.integration.testing.util.ByteBuddyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for creating {@link PojoAccessor} instances that provide reflective access to POJO properties.
 * <p>
 * Introspects class hierarchies to discover non-static fields and caches the resulting metadata
 * per class for efficient reuse. Fields are made accessible via {@link io.github.dimkich.integration.testing.util.ByteBuddyUtils}.
 */
public class PojoAccessorService {
    private final Map<Class<?>, Map<String, PojoProperty>> classMetadataCache = new ConcurrentHashMap<>();

    /**
     * Creates a {@link PojoAccessor} for the given bean instance.
     *
     * @param bean the target object; may be {@code null}
     * @return a {@link PojoAccessor} for the bean, or {@code null} if {@code bean} is {@code null}
     */
    public PojoAccessor forBean(Object bean) {
        if (bean == null) {
            return null;
        }
        return new PojoAccessor(bean, getProperties(bean.getClass()));
    }

    /**
     * Returns the property metadata for the given class, computing it via introspection if not cached.
     *
     * @param cls the class to introspect
     * @return an unmodifiable map of property names to {@link PojoProperty} descriptors
     */
    private Map<String, PojoProperty> getProperties(Class<?> cls) {
        return classMetadataCache.computeIfAbsent(cls, this::introspect);
    }

    /**
     * Introspects the class hierarchy to discover non-static fields.
     * Subclass fields take precedence over superclass fields with the same name.
     *
     * @param cls the class to introspect
     * @return an unmodifiable map of property names to {@link PojoProperty} descriptors
     */
    private Map<String, PojoProperty> introspect(Class<?> cls) {
        Map<String, PojoProperty> props = new LinkedHashMap<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!props.containsKey(field.getName()) && !Modifier.isStatic(field.getModifiers())) {
                    ByteBuddyUtils.makeAccessible(field);
                    props.put(field.getName(), new PojoProperty(field));
                }
            }
            current = current.getSuperclass();
        }
        return Collections.unmodifiableMap(props);
    }
}