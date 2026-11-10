package io.github.dimkich.integration.testing.format.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.type.MapType;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

public class JacksonUtils {
    private enum JsonShape {
        UNKNOWN, SCALAR, STRUCTURED
    }

    public static String getCurrentName(JsonGenerator gen) {
        JsonStreamContext context = gen.getOutputContext();
        while (context.getCurrentName() == null) {
            context = context.getParent();
        }
        return context.getCurrentName();
    }

    public static JsonSerializer<?> decorate(JsonSerializer<?> serializer,
                                             Function<JsonSerializer<?>, JsonSerializer<?>> converter) {
        JsonSerializer<?> previous = null;
        JsonSerializer<?> result = serializer;
        while (serializer != null) {
            JsonSerializer<?> ser = converter.apply(serializer);
            if (ser != null) {
                if (previous != null) {
                    previous.replaceDelegatee(ser);
                } else {
                    result = ser;
                }
                serializer = ser.getDelegatee().getDelegatee();
                previous = ser.getDelegatee();
            } else {
                previous = serializer;
                serializer = serializer.getDelegatee();
            }
        }
        return result;
    }

    public static boolean isIndexedType(Class<?> cls) {
        return (cls.isArray() && cls != byte[].class && cls != char[].class)
                || Collection.class.isAssignableFrom(cls) || Iterable.class.isAssignableFrom(cls)
                || Stream.class.isAssignableFrom(cls) || BaseStream.class.isAssignableFrom(cls)
                || Iterator.class.isAssignableFrom(cls);
    }

    /**
     * Determines whether the type is a collection in the XML polymorphic type-id sense:
     * an array (except {@code byte[]} and {@code char[]}) or a {@link Collection}.
     * <p>
     * This is narrower than {@link #isIndexedType(Class)}: types that merely implement
     * {@link Iterable} but are serialized as beans must not be treated as collections.
     *
     * @param cls the type to inspect
     * @return {@code true} for arrays and collections
     */
    public static boolean isCollectionType(Class<?> cls) {
        return (cls.isArray() && cls != byte[].class && cls != char[].class)
                || Collection.class.isAssignableFrom(cls);
    }

    /**
     * Determines whether the serializer writes a structured value (object, array or map)
     * instead of a scalar.
     * <p>
     * Standard bean and container serializers are detected by their base classes. For custom
     * serializers the Jackson format visitor is used. If the serializer does not declare its
     * shape, the bean description is inspected: {@code @JsonValue} means a scalar, while
     * properties or {@code @JsonAnyGetter} mean a structured value.
     *
     * @param serializer serializer of the type
     * @param provider   serializer provider that created the serializer
     * @param type       Java type being serialized
     * @return {@code true} if the serializer produces an object, array or map
     */
    public static boolean isStructured(JsonSerializer<?> serializer, SerializerProvider provider, JavaType type) {
        if (isStructuredSerializer(serializer)) {
            return true;
        }
        ShapeVisitor visitor = new ShapeVisitor(provider);
        try {
            serializer.acceptJsonFormatVisitor(visitor, type);
        } catch (JsonMappingException e) {
            return isStructuredByBeanDescription(provider, type);
        }
        if (visitor.shape == JsonShape.UNKNOWN) {
            return isStructuredByBeanDescription(provider, type);
        }
        return visitor.shape == JsonShape.STRUCTURED;
    }

    private static boolean isStructuredSerializer(JsonSerializer<?> serializer) {
        while (serializer != null) {
            if (serializer instanceof BeanSerializerBase || serializer instanceof ContainerSerializer) {
                return true;
            }
            serializer = serializer.getDelegatee();
        }
        return false;
    }

    private static boolean isStructuredByBeanDescription(SerializerProvider provider, JavaType type) {
        BeanDescription description = provider.getConfig().introspect(type);
        if (description.findJsonValueAccessor() != null) {
            return false;
        }
        return description.findAnyGetter() != null || !description.findProperties().isEmpty();
    }

    private static class ShapeVisitor extends JsonFormatVisitorWrapper.Base {
        private JsonShape shape = JsonShape.UNKNOWN;

        ShapeVisitor(SerializerProvider provider) {
            super(provider);
        }

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
            setShape(JsonShape.STRUCTURED);
            return new JsonObjectFormatVisitor.Base(getProvider());
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(JavaType type) {
            setShape(JsonShape.STRUCTURED);
            return new JsonArrayFormatVisitor.Base(getProvider());
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(JavaType type) {
            setShape(JsonShape.STRUCTURED);
            return new JsonMapFormatVisitor.Base(getProvider());
        }

        @Override
        public JsonStringFormatVisitor expectStringFormat(JavaType type) {
            setShape(JsonShape.SCALAR);
            return new JsonStringFormatVisitor.Base();
        }

        @Override
        public JsonNumberFormatVisitor expectNumberFormat(JavaType type) {
            setShape(JsonShape.SCALAR);
            return new JsonNumberFormatVisitor.Base();
        }

        @Override
        public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
            setShape(JsonShape.SCALAR);
            return new JsonIntegerFormatVisitor.Base();
        }

        @Override
        public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) {
            setShape(JsonShape.SCALAR);
            return new JsonBooleanFormatVisitor.Base();
        }

        @Override
        public JsonNullFormatVisitor expectNullFormat(JavaType type) {
            setShape(JsonShape.SCALAR);
            return new JsonNullFormatVisitor.Base();
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(JavaType type) {
            return new JsonAnyFormatVisitor.Base();
        }

        private void setShape(JsonShape shape) {
            if (this.shape == JsonShape.UNKNOWN) {
                this.shape = shape;
            }
        }
    }

    public static MapTypes resolveMapTypes(JavaType type) {
        JavaType keyType = null;
        JavaType valueType = null;

        if (type instanceof MapType mapType) {
            keyType = mapType.getKeyType();
            valueType = mapType.getContentType();
        } else if (Iterable.class.isAssignableFrom(type.getRawClass()) ||
                Iterator.class.isAssignableFrom(type.getRawClass())) {

            JavaType entryType = type.containedTypeOrUnknown(0);
            if (entryType != null && Map.Entry.class.isAssignableFrom(entryType.getRawClass())) {
                keyType = entryType.containedTypeOrUnknown(0);
                valueType = entryType.containedTypeOrUnknown(1);
            }
        }
        if (keyType == null || valueType == null) {
            return null;
        }
        return new MapTypes(keyType, valueType);
    }

    // for debug purpose
    @SneakyThrows
    public static void printCurrentToken(JsonParser p) {
        int i = 0;
        JsonStreamContext context = p.getParsingContext();
        if (context == null) {
            return;
        }
        while (context.getParent() != null) {
            context = context.getParent();
            i++;
        }
        if (p.currentToken() == JsonToken.START_OBJECT || p.currentToken() == JsonToken.START_ARRAY) {
            i--;
        }
        System.out.print(" ".repeat(i * 4) + p.currentToken());
        if (p.currentToken() == JsonToken.FIELD_NAME) {
            System.out.print(" " + p.currentName());
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            System.out.print(" " + p.getText());
        }
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            System.out.print(" null");
        }
        System.out.println();
    }
}
