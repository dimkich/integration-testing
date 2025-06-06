package io.github.dimkich.integration.testing.storage;

import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class PojoToMapUtil {
    @SneakyThrows
    public static Map<String, Object> mapWithSpringBeanUtils(Object object) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(object.getClass())) {
            Method method = descriptor.getReadMethod();
            if (method == null) {
                continue;
            }
            map.put(descriptor.getName(), method.invoke(object));
        }
        return map;
    }

    @SneakyThrows
    public static Map<String, Object> mapWithReflectionMethods(Object object) {
        Class<?> cls = object.getClass();
        Method[] methods = cls.getMethods();
        Map<String, Object> map = new LinkedHashMap<>();
        for (Method method : methods) {
            if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && method.getParameterCount() == 0) {
                String name = method.getName().substring(method.getName().startsWith("get") ? 3 : 2);
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                map.put(name, method.invoke(object));
            }
        }
        return map;
    }

    @SneakyThrows
    public static Map<String, Object> mapWithReflectionFields(Object object) {
        Class<?> cls = object.getClass();
        Field[] fields = cls.getDeclaredFields();
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : fields) {
            String name = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            Method method;
            try {
                method = cls.getMethod("get" + name);
            } catch (NoSuchMethodException e) {
                try {
                    method = cls.getMethod("is" + name);
                } catch (NoSuchMethodException ignore) {
                    continue;
                }
            }
            if (method.getParameterCount() > 0) {
                continue;
            }
            map.put(field.getName(), method.invoke(object));
        }
        return map;
    }
}
