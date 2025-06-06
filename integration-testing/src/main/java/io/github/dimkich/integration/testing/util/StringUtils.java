package io.github.dimkich.integration.testing.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class StringUtils {
    private static final ToStringStyle toString = new CustomToStringStyle();

    public static String objectToString(Object object) {
        if (object == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (object instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return "[]";
            }
            builder.append("[");
            for (Object o : collection) {
                builder.append(objectToString(o)).append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());
            builder.append("]");
            return builder.toString();
        }
        if (object instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "[]";
            }
            builder.append("[");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                builder.append(objectToString(entry.getKey())).append('=').append(objectToString(entry.getValue())).append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());
            builder.append("]");
            return builder.toString();
        }

        if (object.getClass().getName().startsWith("java.")) {
            return object.toString();
        }

        CustomReflectionToStringBuilder toStringBuilder = new CustomReflectionToStringBuilder(object, toString);
        builder.append(toStringBuilder);

        return builder.toString();
    }


    private static class CustomReflectionToStringBuilder extends ReflectionToStringBuilder {
        public CustomReflectionToStringBuilder(Object object, ToStringStyle style) {
            super(object, style);
        }

        @Override
        @SneakyThrows
        protected boolean accept(Field field) {
            if (super.accept(field)) {
                Object value = field.get(getObject());
                if (value instanceof Collection<?> collection) {
                    return !collection.isEmpty();
                }
                if (value instanceof Map<?, ?> map) {
                    return !map.isEmpty();
                }
                return value != null;
            }
            return false;
        }

        @Override
        public String toString() {
            if (getObject() != null && Collection.class.isAssignableFrom(getObject().getClass())) {
                this.reflectionAppendArray(((Collection<?>) this.getObject()).toArray());
                this.getStringBuffer().delete(0, 1);
                return this.getStringBuffer().toString();
            }
            if (getObject() != null && Enum.class.isAssignableFrom(getObject().getClass())) {
                return ((Enum<?>)getObject()).name();
            }
            try {
                return super.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class CustomToStringStyle extends ToStringStyle {

        public CustomToStringStyle() {
            setUseClassName(false);
            setUseIdentityHashCode(false);
            setContentStart("{");
            setContentEnd("}");
            setArrayStart("[");
            setArrayEnd("]");
        }

        @Override
        public void appendDetail(final StringBuffer buffer, final String fieldName, final Object value) {
            if (value.getClass().getName().startsWith("java.")) {
                super.appendDetail(buffer, fieldName, value);
            } else {
                buffer.append(StringUtils.objectToString(value));
            }
        }

        @Override
        protected void appendDetail(final StringBuffer buffer, final String fieldName, final Collection<?> coll) {
            appendClassName(buffer, coll);
            appendIdentityHashCode(buffer, coll);
            appendDetail(buffer, fieldName, coll.toArray());
        }
    }
}
