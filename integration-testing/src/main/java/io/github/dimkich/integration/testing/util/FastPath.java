package io.github.dimkich.integration.testing.util;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.BooleanSerializer;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializers;
import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer;
import com.fasterxml.jackson.databind.util.RawValue;
import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.MonthDay;
import java.time.Period;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;

/**
 * Answers the type and container questions of the difference engine.
 * <p>
 * The class is the single place for two kinds of decisions:
 * <ul>
 *     <li>semantic classification that defines the result: whether a type is a scalar, whether a
 *         container keeps a meaningful order, and whether two values can be compared by
 *         {@code equals};</li>
 *     <li>shortcuts that only avoid work without changing the result: whether a serializer can
 *         produce text directly and how an embedded value is rendered.</li>
 * </ul>
 * The questions are stateless: the class has no Spring bean and no mutable state; the only cache is
 * the immutable per-class classification of equality types. The context that a question needs —
 * the current format's {@link SimpleTypeDetector} or a Jackson {@link SerializerProvider} — is
 * passed explicitly.
 * <p>
 * A shortcut must not change the meaning of an operation: it may only answer that a cheaper path
 * produces exactly the same result as the general one. The ordering and equality questions are not
 * shortcuts: they define the output and the comparison.
 */
public final class FastPath {
    private static final Set<Class<?>> SCALAR_TYPES = Set.of(
            String.class, Boolean.class, Character.class,
            Integer.class, Long.class, Short.class, Byte.class);

    private static final ClassValue<Boolean> COMPARABLE_BY_EQUALS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(@NonNull Class<?> type) {
            return type.isEnum() || SCALAR_TYPES.contains(type) || valueEquals(type);
        }
    };

    private FastPath() {
    }

    /**
     * @param type type to inspect
     * @return {@code true} when the type is an array
     */
    public static boolean isArray(Class<?> type) {
        return type.isArray();
    }

    /**
     * @param type     type to inspect
     * @param detector detector of the current test format
     * @return {@code true} when Jackson writes the type as a scalar
     */
    public static boolean isScalar(Class<?> type, SimpleTypeDetector detector) {
        return detector.isSimple(type);
    }

    /**
     * @param type     type to inspect
     * @param detector detector of the current test format
     * @param provider serializer provider of the mapper that serializes the value
     * @return {@code true} when Jackson writes the type as a scalar
     */
    public static boolean isScalar(Class<?> type, SimpleTypeDetector detector,
                                   SerializerProvider provider) {
        return detector.isSimple(type, provider);
    }

    /**
     * @param type     type to inspect
     * @param detector detector of the current test format
     * @return {@code true} when the presenter may keep the value as a raw body
     */
    public static boolean isScalarOrArray(Class<?> type, SimpleTypeDetector detector) {
        return type.isArray() || detector.isSimple(type);
    }

    /**
     * Reports whether a value is written as a bean or a container rather than unwrapped as an array
     * or a scalar.
     *
     * @param type     type to inspect
     * @param detector detector of the current test format
     * @param provider serializer provider of the mapper that serializes the value
     * @return {@code true} when the value keeps its own structure
     */
    public static boolean isStructuredValue(Class<?> type, SimpleTypeDetector detector,
                                            SerializerProvider provider) {
        if (type.isArray()) {
            return false;
        }
        if (Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)
                || Iterable.class.isAssignableFrom(type) || Iterator.class.isAssignableFrom(type)) {
            return true;
        }
        return !detector.isSimple(type, provider);
    }

    /**
     * Reports whether values of the type have a content-based {@code equals} that can replace the
     * recursive comparison. A registered equality predicate is not considered here: the predicate
     * still has to be applied by its owner.
     *
     * @param type type to inspect
     * @return {@code true} when the type is an enum, a scalar or a JDK value type
     */
    public static boolean isComparableByEquals(Class<?> type) {
        return COMPARABLE_BY_EQUALS.get(type);
    }

    /**
     * Reports whether a map keeps an order of its own: insertion order for a linked map,
     * comparator order for a sorted map, declaration order for an enum map. An empty map has no
     * keys to order, so it does not force sorting on the other side.
     *
     * @param map map to inspect
     * @return {@code true} when the iteration order of the map is meaningful
     */
    public static boolean isOrdered(Map<?, ?> map) {
        if (map.isEmpty()) {
            return true;
        }
        return map instanceof LinkedHashMap<?, ?>
                || map instanceof LinkedMultiValueMap<?, ?>
                || map instanceof SortedMap<?, ?>
                || map instanceof EnumMap<?, ?>;
    }

    /**
     * Reports whether a collection keeps an order of its own: a list, a linked set, a sorted set,
     * an enum set or a deque.
     *
     * @param collection collection to inspect
     * @return {@code true} when the iteration order of the collection is meaningful
     */
    public static boolean isOrdered(Collection<?> collection) {
        if (collection.isEmpty()) {
            return true;
        }
        return collection instanceof List<?>
                || collection instanceof LinkedHashSet<?>
                || collection instanceof SortedSet<?>
                || collection instanceof EnumSet<?>
                || collection instanceof Deque<?>;
    }

    /**
     * Initial capacity of a hash map or set that receives exactly the given number of entries
     * without resizing.
     *
     * @param expectedSize expected number of entries
     * @return initial capacity
     */
    public static int hashCapacity(int expectedSize) {
        return (int) (expectedSize / 0.75f) + 1;
    }

    /**
     * Reads the text of a value whose standard Jackson serializer is known to produce the same
     * text directly. The buffer is skipped only for the exact standard serializer class, so a
     * custom serializer of the same value type keeps its behavior.
     *
     * @param serializer serializer of the value
     * @param value      value to render
     * @param provider   serializer provider of the current format
     * @return text of the value, or {@code null} when the serializer has no direct fast path
     */
    public static String standardText(JsonSerializer<?> serializer, Object value,
                                      SerializerProvider provider) {
        Class<?> serializerType = serializer.getClass();
        if (serializerType == ByteArraySerializer.class) {
            return provider.getConfig().getBase64Variant().encode((byte[]) value, false, "\n");
        }
        if (serializerType == StdArraySerializers.CharArraySerializer.class) {
            return new String((char[]) value);
        }
        if (serializerType == UUIDSerializer.class) {
            return value.toString();
        }
        if (serializerType == NumberSerializers.IntegerSerializer.class) {
            return Integer.toString((Integer) value);
        }
        if (serializerType == NumberSerializers.LongSerializer.class) {
            return Long.toString((Long) value);
        }
        if (serializerType == NumberSerializers.ShortSerializer.class) {
            return Short.toString((Short) value);
        }
        if (serializerType == NumberSerializers.IntLikeSerializer.class) {
            return Integer.toString(((Number) value).intValue());
        }
        if (serializerType == NumberSerializers.FloatSerializer.class) {
            return Float.toString((Float) value);
        }
        if (serializerType == NumberSerializers.DoubleSerializer.class) {
            return Double.toString((Double) value);
        }
        if (serializerType == BooleanSerializer.class) {
            return Boolean.toString((Boolean) value);
        }
        if (serializerType == ToStringSerializer.class) {
            return value.toString();
        }
        if (serializerType == NumberSerializer.class && value instanceof BigInteger integer) {
            return integer.toString();
        }
        return null;
    }

    /**
     * Reads a value that a serializer wrote as an embedded object rather than as a token.
     *
     * @param embedded value of the embedded token
     * @param provider serializer provider of the current format
     * @return text of the embedded value
     */
    public static String embeddedText(Object embedded, SerializerProvider provider) {
        if (embedded instanceof byte[] bytes) {
            // A linefeed is written as real text, not as the JSON escape sequence the variant
            // would produce by default.
            return provider.getConfig().getBase64Variant().encode(bytes, false, "\n");
        }
        if (embedded instanceof RawValue rawValue) {
            return String.valueOf(rawValue.rawValue());
        }
        return String.valueOf(embedded);
    }

    /**
     * Reports whether a JDK type is a value type with content-based {@code equals}. Such types are
     * compared by {@code equals} instead of a recursive walk: the walk costs far more and gives the
     * same result for an immutable value. User types are not included, because their fields may
     * need registered equality predicates.
     */
    private static boolean valueEquals(Class<?> type) {
        return type == UUID.class
                || type == BigInteger.class
                || type == Duration.class
                || type == Period.class
                || type == MonthDay.class
                || Date.class.isAssignableFrom(type)
                || ZoneId.class.isAssignableFrom(type)
                || File.class.isAssignableFrom(type)
                || Path.class.isAssignableFrom(type)
                || URI.class.isAssignableFrom(type)
                || Locale.class.isAssignableFrom(type)
                || Currency.class.isAssignableFrom(type);
    }
}
