package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.TestPart;
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

            compConfig.registerEqualsForType((o1, o2) -> true, SecureRandom.class);
            compConfig.registerEqualsForType((o1, o2) -> Arrays.equals(((ByteArrayResource) o1).getByteArray(),
                    ((ByteArrayResource) o2).getByteArray()), sr2Class);
            compConfig.registerEqualsForType((o1, o2) -> o1.stripTrailingZeros().equals(o2.stripTrailingZeros()),
                    BigDecimal.class);
            compConfig.compareOnlyFieldsOfTypes();

            compConfig.registerEqualsForType(equalsIgnoreFields("backtrace", "statusText"),
                    HttpClientErrorException.BadRequest.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("type"), RequestEntity.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber"), TestContainer.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber"), TestCase.class);
            compConfig.registerEqualsForType(equalsIgnoreFields("columnNumber", "lineNumber"), TestPart.class);

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
}
