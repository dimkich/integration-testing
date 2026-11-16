package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.TestPart;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffNullValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffScalarValue;
import io.github.dimkich.integration.testing.storage.diff.model.StorageDiff;
import lombok.SneakyThrows;
import org.assertj.core.api.recursive.comparison.ComparisonDifference;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class FormatTestUtils {
    public static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    public static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();
    private static final Constructor<?> sr2;

    static {
        try {
            Class<?> sr2Class = Class.forName("org.springframework.http.converter.ResourceHttpMessageConverter$2");

            compConfig.registerEqualsForType((o1, o2) -> o1 != null && o2 != null, SecureRandom.class);
            compConfig.registerEqualsForType((o1, o2) -> Arrays.equals(((ByteArrayResource) o1).getByteArray(),
                    ((ByteArrayResource) o2).getByteArray()), sr2Class);
            compConfig.registerEqualsForType((o1, o2) -> o1.stripTrailingZeros().equals(o2.stripTrailingZeros()),
                    BigDecimal.class);
            compConfig.compareOnlyFieldsOfTypes();

            compConfig.registerEqualsForType(equalsIgnoreFields("backtrace", "statusText"),
                    HttpClientErrorException.BadRequest.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("type"), RequestEntity.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber", "inits.test"),
                    TestContainer.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber", "inits.test"),
                    TestCase.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber", "inits.test"),
                    TestPart.class);
            compConfig.registerEqualsForType((it1, it2) -> {
                if (it1 == it2) return true;
                if (it1 == null || it2 == null) return false;

                while (it1.hasNext() && it2.hasNext()) {
                    Object o1 = it1.next();
                    Object o2 = it2.next();
                    if (!(Objects.equals(o1, o2))) {
                        return false;
                    }
                }
                return !it1.hasNext() && !it2.hasNext();
            }, Iterator.class);

            sr2 = sr2Class.getDeclaredConstructor(ResourceHttpMessageConverter.class, byte[].class,
                    HttpInputMessage.class);
            sr2.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static HttpHeaders httpHeaders(Object... objects) {
        HttpHeaders httpHeaders = new HttpHeaders();
        Iterator<Object> iterator = Arrays.stream(objects).iterator();
        while (iterator.hasNext()) {
            httpHeaders.addAll((String) iterator.next(), (List<? extends String>) iterator.next());
        }
        return httpHeaders;
    }

    @SneakyThrows
    public static Resource sr2(byte[] data) {
        return (Resource) sr2.newInstance(Mockito.mock(ResourceHttpMessageConverter.class), data,
                Mockito.mock(HttpInputMessage.class));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... keyValues) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }

    public static <T> BiPredicate<? super T, ? super T> equalsIgnoreFields(String... fields) {
        RecursiveComparisonConfiguration config = new RecursiveComparisonConfiguration();
        config.ignoreFields(fields);
        return (o1, o2) -> {
            List<ComparisonDifference> diff = compCalculator.determineDifferences(o1, o2, config);
            if (!diff.isEmpty()) {
                System.out.println(diff.stream().map(Objects::toString).collect(Collectors.joining("\n")));
            }
            return diff.isEmpty();
        };
    }

    /**
     * A difference with an inserted record, an updated record and a deleted record: the inserted
     * record carries a snapshot body, the updated one a difference body.
     */
    public static TestCase diffInsertUpdateDelete() {
        DiffEntry inserted = new DiffEntry(DiffOperation.INSERT, "users/1", "User", Map.of(),
                new DiffObjectValue(List.of(scalarEntry("name", "Ivan"))));
        DiffEntry updated = DiffEntry.of(DiffOperation.UPDATE, "orders/100",
                new DiffObjectValue(List.of(scalarEntry("status", "PAID"))));
        DiffEntry deleted = DiffEntry.of(DiffOperation.DELETE, "orders/101", null);
        return diffTestCase("diff insert, update and delete", inserted, updated, deleted);
    }

    /**
     * A changed field with an explicit null: the element carries {@code xsi:nil} and the
     * {@code nil} attribute must not be confused with a child element.
     */
    public static TestCase diffNullValue() {
        DiffEntry updated = DiffEntry.of(DiffOperation.UPDATE, "orders/100",
                new DiffObjectValue(List.of(fieldEntry("note", new DiffNullValue()))));
        return diffTestCase("diff null value", updated);
    }

    /**
     * Data fields named exactly as the service attributes: the record has {@code key} and
     * {@code type} attributes, and its body contains fields with the same names.
     */
    public static TestCase diffAttributeNames() {
        DiffEntry updated = new DiffEntry(DiffOperation.UPDATE, "orders/100", "Order", Map.of(),
                new DiffObjectValue(List.of(
                        scalarEntry("key", "abc"),
                        scalarEntry("type", "LIMIT"))));
        return diffTestCase("diff fields named as attributes", updated);
    }

    /**
     * A nested object with a changed field and an added field: the difference body keeps its shape
     * at every level.
     */
    public static TestCase diffNestedObject() {
        DiffEntry age = new DiffEntry(DiffOperation.INSERT, "age", "Integer", Map.of(), new DiffScalarValue("5"));
        DiffEntry profile = fieldEntry("profile", new DiffObjectValue(List.of(
                scalarEntry("name", "Ivan"),
                age)));
        DiffEntry updated = DiffEntry.of(DiffOperation.UPDATE, "orders/100", new DiffObjectValue(List.of(profile)));
        return diffTestCase("diff nested object", updated);
    }

    /**
     * A difference with an object key: the key is written as a structure, the value as a snapshot
     * and the deleted element keeps only the key.
     */
    public static TestCase diffObjectKey() {
        DiffObjectKey key = new DiffObjectKey("MDKey", Map.of(),
                new DiffObjectValue(List.of(
                        scalarEntry("exchange", "MOEX"),
                        scalarEntry("classCode", "TQCB"),
                        scalarEntry("ticker", "SBER"))));
        DiffEntry inserted = new DiffEntry(DiffOperation.INSERT, key, "MD", Map.of(),
                new DiffObjectValue(List.of(
                        new DiffEntry(null, "closePrice", "BigDecimal", Map.of(),
                                new DiffScalarValue("259.43")))));
        DiffEntry deleted = new DiffEntry(DiffOperation.DELETE, key, null, Map.of(), null);
        return diffTestCase("diff object key", inserted, deleted);
    }

    /**
     * A difference with a text key and a body field named {@code key}: the key attribute wins
     * over the child element even when the child is the only one.
     */
    public static TestCase diffKeyAttributeAndKeyField() {
        DiffEntry updated = new DiffEntry(DiffOperation.UPDATE, "orders/100", "Order", Map.of(),
                new DiffObjectValue(List.of(scalarEntry("key", "abc"))));
        return diffTestCase("diff key attribute and key field", updated);
    }

    /**
     * A snapshot insert without a key and with a body field named {@code key}: the first child
     * stays an ordinary field because it is not followed by a {@code <value>} wrapper.
     */
    public static TestCase diffKeylessInsertWithKeyField() {
        DiffEntry inserted = new DiffEntry(DiffOperation.INSERT, (String) null, "Order", Map.of(),
                new DiffObjectValue(List.of(
                        scalarEntry("key", "abc"),
                        scalarEntry("status", "PAID"))));
        return diffTestCase("diff keyless insert with key field", inserted);
    }

    /**
     * A difference with an object key and a scalar value: the value is written into the
     * {@code <value>} wrapper as text.
     */
    public static TestCase diffObjectKeyScalarValue() {
        DiffObjectKey key = new DiffObjectKey("MDKey", Map.of(),
                new DiffObjectValue(List.of(scalarEntry("ticker", "SBER"))));
        DiffEntry inserted = new DiffEntry(DiffOperation.INSERT, key, "MD", Map.of(),
                new DiffScalarValue("one"));
        return diffTestCase("diff object key with scalar value", inserted);
    }

    private static TestCase diffTestCase(String name, DiffEntry... entries) {
        TestCase test = new TestCase();
        test.setName(name);
        test.setDiff(List.of(new StorageDiff("dataSource", List.of(entries))));
        return test;
    }

    private static DiffEntry scalarEntry(String name, String text) {
        return fieldEntry(name, new DiffScalarValue(text));
    }

    private static DiffEntry fieldEntry(String name, DiffValue value) {
        return DiffEntry.of(null, name, value);
    }
}
