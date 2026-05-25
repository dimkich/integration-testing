package io.github.dimkich.integration.testing.config;

import io.github.sugarcubes.cloner.Cloner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Merges properties from a source bean into a target bean with support for
 * inheritance semantics and exclusive property groups.
 * <p>
 * Non-null properties in {@code source} are copied to {@code target} when the
 * corresponding target property is null. For non-null target properties,
 * values are deep-merged recursively: {@link Set}s are unioned (new elements
 * from source are added), {@link Map}s are merged by key, and other bean-like
 * objects are merged via {@link #merge(Object, Object)}. Simple value types
 * and null source values are left unchanged.
 * <p>
 * Fields annotated with {@link PropertyInheritanceExclusive} form exclusive
 * groups. If the target has already set any property in a group, other null
 * properties in that group will not inherit from the source, preventing
 * mixing values from different levels within a logical choice.
 * <p>
 * This class is thread-safe. Exclusivity metadata is cached per class.
 *
 * @see PropertyInheritanceExclusive
 */
@RequiredArgsConstructor
public class PropertyInheritanceMerger {
    private final Cloner cloner;
    private final Map<Class<?>, Map<String, String>> exclusivityCache = new ConcurrentHashMap<>();

    /**
     * Merges properties from {@code source} into {@code target}.
     * <p>
     * Only writable properties in target and readable in source are considered.
     * Null source values are skipped. For null target values, the source value
     * is cloned and set. For non-null target values, deep merge is performed.
     *
     * @param target the bean to receive merged properties; not modified if null
     * @param source the bean to copy properties from; ignored if null
     */
    public void merge(Object target, Object source) {
        if (target == null || source == null) {
            return;
        }

        BeanWrapper targetWrap = PropertyAccessorFactory.forBeanPropertyAccess(target);
        BeanWrapper sourceWrap = PropertyAccessorFactory.forBeanPropertyAccess(source);

        Map<String, String> fieldToGroup = exclusivityCache.computeIfAbsent(
                target.getClass(),
                this::buildExclusivityMap
        );

        Set<String> groupsDefinedByChild = new HashSet<>();
        fieldToGroup.forEach((fieldName, groupName) -> {
            if (targetWrap.getPropertyValue(fieldName) != null) {
                groupsDefinedByChild.add(groupName);
            }
        });

        for (PropertyDescriptor pd : targetWrap.getPropertyDescriptors()) {
            String name = pd.getName();
            if (!targetWrap.isWritableProperty(name) || !sourceWrap.isReadableProperty(name)) {
                continue;
            }
            Object targetValue = targetWrap.getPropertyValue(name);
            Object sourceValue = sourceWrap.getPropertyValue(name);
            if (sourceValue == null) {
                continue;
            }

            String groupName = fieldToGroup.get(name);
            if (groupName != null && targetValue == null) {
                if (groupsDefinedByChild.contains(groupName)) {
                    continue;
                }
            }

            if (targetValue == null) {
                targetWrap.setPropertyValue(name, cloner.clone(sourceValue));
            } else {
                deepMerge(targetValue, sourceValue);
            }
        }
    }

    /**
     * Builds a map from field name to exclusive group name for fields annotated
     * with {@link PropertyInheritanceExclusive}.
     *
     * @param cls the class to scan for annotated fields
     * @return map of field name to group name; never null
     */
    private Map<String, String> buildExclusivityMap(Class<?> cls) {
        Map<String, String> map = new HashMap<>();
        ReflectionUtils.doWithFields(cls, field -> {
            PropertyInheritanceExclusive ann = field.getAnnotation(PropertyInheritanceExclusive.class);
            if (ann != null) {
                map.put(field.getName(), ann.value());
            }
        });
        return map;
    }

    /**
     * Recursively merges {@code source} into {@code target} depending on type.
     * Sets are unioned (source elements not in target are cloned and added).
     * Maps are merged by key via {@link #mergeMaps}. Other non-simple beans
     * are merged via {@link #merge(Object, Object)}.
     *
     * @param target the object to merge into
     * @param source the object to merge from
     */
    @SuppressWarnings("unchecked")
    private void deepMerge(Object target, Object source) {
        if (target instanceof Set && source instanceof Set) {
            Set<Object> targetSet = (Set<Object>) target;
            Set<Object> sourceSet = (Set<Object>) source;
            for (Object s : sourceSet) {
                if (!targetSet.contains(s)) {
                    targetSet.add(cloner.clone(s));
                }
            }
        } else if (target instanceof Map && source instanceof Map) {
            mergeMaps((Map<Object, Object>) target, (Map<Object, Object>) source);
        } else if (!BeanUtils.isSimpleValueType(target.getClass())) {
            merge(target, source);
        }
    }

    /**
     * Merges {@code source} map entries into {@code target}. For keys not in
     * target, the key-value pair is cloned and added. For existing keys,
     * values are deep-merged.
     *
     * @param target the map to merge into
     * @param source the map to merge from
     */
    private void mergeMaps(Map<Object, Object> target, Map<Object, Object> source) {
        source.forEach((k, v) -> {
            if (v == null) {
                return;
            }
            Object targetVal = target.get(k);
            if (targetVal == null) {
                target.put(cloner.clone(k), cloner.clone(v));
            } else {
                deepMerge(targetVal, v);
            }
        });
    }
}