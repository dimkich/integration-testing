package io.github.dimkich.integration.testing.format;

import org.assertj.core.api.recursive.comparison.ComparisonDifference;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class FormatTestUtils {
    public static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    public static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    static {
        compConfig.registerEqualsForType((o1, o2) -> true, SecureRandom.class);
        compConfig.registerEqualsForType((o1, o2) -> o1.stripTrailingZeros().equals(o2.stripTrailingZeros()),
                BigDecimal.class);

        RecursiveComparisonConfiguration exceptionConfig = new RecursiveComparisonConfiguration();
        exceptionConfig.ignoreFields("backtrace", "statusText");
        compConfig.registerEqualsForType((o1, o2) -> {
                    List<ComparisonDifference> diff = compCalculator.determineDifferences(o1, o2, exceptionConfig);
                    if (!diff.isEmpty()) {
                        System.out.println(diff.stream().map(Objects::toString).collect(Collectors.joining("\n")));
                    }
                    return diff.isEmpty();
                },
                HttpClientErrorException.BadRequest.class);
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

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... keyValues) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }
}
