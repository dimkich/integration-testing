package io.github.dimkich.integration.testing.redis.spring.ttl;

import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.util.Pair;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;

public class TtlEntityData {
    private PropertyDescriptor timeToLiveProperty;
    private TimeToLive timeToLive;
    private PropertyDescriptor idProperty;

    public static TtlEntityData tryCreateFromClass(Class<?> cls) {
        Pair<PropertyDescriptor, TimeToLive> ttl = find(cls, TimeToLive.class);
        if (ttl != null) {
            TtlEntityData data = new TtlEntityData();
            data.timeToLiveProperty = ttl.getFirst();
            data.timeToLive = ttl.getSecond();
            Pair<PropertyDescriptor, Id> id = find(cls, Id.class);
            if (id != null) {
                data.idProperty = id.getFirst();
            }
            return data;
        }
        return null;
    }

    @SneakyThrows
    public Number getTtl(Object entity) {
        return (Number) timeToLiveProperty.getReadMethod().invoke(entity);
    }

    @SneakyThrows
    public void setTtl(Object entity, Number ttl) {
        Class<?> cls = timeToLiveProperty.getWriteMethod().getParameterTypes()[0];
        if (cls == Integer.class || cls == int.class) {
            timeToLiveProperty.getWriteMethod().invoke(entity, ttl == null ? null : ttl.intValue());
        } else {
            timeToLiveProperty.getWriteMethod().invoke(entity, ttl);
        }
    }

    @SneakyThrows
    public Object getId(Object entity) {
        return idProperty.getReadMethod().invoke(entity);
    }

    public ChronoUnit getChronoUnit() {
        return timeToLive.unit().toChronoUnit();
    }

    private static <A extends Annotation> Pair<PropertyDescriptor, A> find(Class<?> cls, Class<A> annotationClass) {
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotationClass)) {
                PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(cls, field.getName());
                if (pd != null) {
                    return Pair.of(pd, field.getAnnotation(annotationClass));
                }
            }
        }
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
                if (pd != null) {
                    return Pair.of(pd, method.getAnnotation(annotationClass));
                }
            }
        }
        return null;
    }
}
