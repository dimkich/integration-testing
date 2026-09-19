package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import io.github.dimkich.integration.testing.kafka.inflight.interceptor.InFlightConsumerInterceptor;
import io.github.dimkich.integration.testing.kafka.inflight.interceptor.InFlightProducerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.*;

/**
 * Produces a copy of a Kafka client config with the in-flight interceptor
 * class appended to {@code interceptor.classes}.
 *
 * <p><b>Non-mutating contract.</b> The input config is never modified. This is
 * deliberate: users may pass immutable maps (e.g. {@code Map.of(...)}), may
 * reuse the same config object for several clients, and may inspect the config
 * after construction. Mutating in place would (a) throw
 * {@link UnsupportedOperationException} on immutable maps, (b) leak the injected
 * class into unrelated clients sharing the same map, and (c) expose framework
 * internals in user-visible state.
 *
 * <p>{@link Properties} is checked before {@link Map}: {@code Properties} is a
 * {@code Map} subclass, and the instrumented {@code KafkaProducer}/{@code KafkaConsumer}
 * constructors that accept {@code Properties} declare it as the parameter type.
 * The replacement value is written back into that slot, so it must be a
 * {@code Properties} in that case.
 *
 * <p>The copy preserves the original config's entry order (via
 * {@link LinkedHashMap}) and is handed back to the instrumented constructor,
 * which is the object Kafka actually reads.
 *
 * <p>Only the interceptor <em>class</em> is injected
 * ({@code interceptor.classes}); no per-client id is written into the config.
 * The producer interceptor derives its identity from the user-supplied
 * {@code transactional.id}.
 */
public class InterceptorInjector {

    /**
     * Returns a copy of the producer config with {@link InFlightProducerInterceptor}
     * merged into {@code interceptor.classes}. The original object is untouched.
     *
     * @param config the config passed as the first argument to a {@code KafkaProducer}
     *               constructor; either a {@link Properties}, a {@link Map}, or an
     *               unrecognized object (returned as-is)
     * @return a new config instance, or the input unchanged when it is neither
     *         {@code Map} nor {@code Properties}
     */
    public static Object prepareProducerConfig(Object config) {
        if (config instanceof Properties props) {
            Properties copy = new Properties();
            copy.putAll(props);
            injectIntoProducerProperties(copy);
            return copy;
        }
        if (config instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>(castMap(map));
            injectIntoProducerMap(copy);
            return copy;
        }
        return config;
    }

    /**
     * Returns a copy of the consumer config with {@link InFlightConsumerInterceptor}
     * merged into {@code interceptor.classes}. The original object is untouched.
     *
     * @param config the config passed as the first argument to a {@code KafkaConsumer}
     *               constructor; either a {@link Properties}, a {@link Map}, or an
     *               unrecognized object (returned as-is)
     * @return a new config instance, or the input unchanged when it is neither
     *         {@code Map} nor {@code Properties}
     */
    public static Object prepareConsumerConfig(Object config) {
        if (config instanceof Properties props) {
            Properties copy = new Properties();
            copy.putAll(props);
            injectIntoConsumerProperties(copy);
            return copy;
        }
        if (config instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>(castMap(map));
            injectIntoConsumerMap(copy);
            return copy;
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        // Kafka config maps are always Map<String, Object> by contract; a
        // defensive copy via the wildcard constructor keeps the cast safe and
        // avoids trusting the user's generic signature.
        return (Map<String, Object>) map;
    }

    // ---------------------------------------------------------------------
    // Mutation of copies
    // ---------------------------------------------------------------------

    /**
     * Merges {@link InFlightProducerInterceptor} into a producer config {@code Map},
     * normalizing the existing {@code interceptor.classes}. The result is stored as
     * a {@code List}.
     */
    @SuppressWarnings("unchecked")
    private static void injectIntoProducerMap(Map<String, Object> config) {
        String ours = InFlightProducerInterceptor.class.getName();
        Object current = config.get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        List<String> existing = current instanceof List
                ? new ArrayList<>((List<String>) current)
                : parseInterceptors((String) current);
        config.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                mergeInterceptors(existing, ours));
    }

    /**
     * Merges {@link InFlightProducerInterceptor} into a producer config
     * {@code Properties}, keeping the {@code interceptor.classes} value a
     * comma-separated string.
     */
    private static void injectIntoProducerProperties(Properties config) {
        String ours = InFlightProducerInterceptor.class.getName();
        String current = config.getProperty(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        config.setProperty(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                joinInterceptors(mergeInterceptors(parseInterceptors(current), ours)));
    }

    /**
     * Merges {@link InFlightConsumerInterceptor} into a consumer config {@code Map},
     * normalizing the existing {@code interceptor.classes}. The result is stored as
     * a {@code List}.
     */
    @SuppressWarnings("unchecked")
    private static void injectIntoConsumerMap(Map<String, Object> config) {
        String ours = InFlightConsumerInterceptor.class.getName();
        Object current = config.get(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
        List<String> existing = current instanceof List
                ? new ArrayList<>((List<String>) current)
                : parseInterceptors((String) current);
        config.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                mergeInterceptors(existing, ours));
    }

    /**
     * Merges {@link InFlightConsumerInterceptor} into a consumer config
     * {@code Properties}, keeping the {@code interceptor.classes} value a
     * comma-separated string. Repeated injection is idempotent: the class is only
     * appended when it is not already present by exact class name.
     */
    private static void injectIntoConsumerProperties(Properties config) {
        String ours = InFlightConsumerInterceptor.class.getName();
        String current = config.getProperty(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
        config.setProperty(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                joinInterceptors(mergeInterceptors(parseInterceptors(current), ours)));
    }

    // ---------------------------------------------------------------------
    // Interceptor list normalization
    // ---------------------------------------------------------------------

    /**
     * Parses a comma-separated interceptor class list into a whitespace-trimmed
     * list, dropping empty tokens. Deduplication is deferred to
     * {@link #mergeInterceptors}.
     */
    private static List<String> parseInterceptors(String commaSeparated) {
        List<String> result = new ArrayList<>();
        if (commaSeparated == null || commaSeparated.isEmpty()) {
            return result;
        }
        for (String token : commaSeparated.split(",")) {
            if (!token.trim().isEmpty()) {
                result.add(token.trim());
            }
        }
        return result;
    }

    /**
     * Normalizes an interceptor class list: trims tokens, drops duplicates
     * (preserving first-seen order), and appends {@code ours} unless already present
     * by exact class name. Exact matching makes repeated injection idempotent without
     * treating a substring of another class name as a hit.
     *
     * @param existing the existing class names, already trimmed and de-duplicated by
     *        the caller where relevant
     * @param ours the interceptor class to guarantee present at the end of the list
     */
    private static List<String> mergeInterceptors(List<String> existing, String ours) {
        List<String> result = new ArrayList<>();
        for (String raw : existing) {
            String name = raw.trim();
            if (name.isEmpty() || result.contains(name)) {
                continue;
            }
            result.add(name);
        }
        if (!result.contains(ours)) {
            result.add(ours);
        }
        return result;
    }

    private static String joinInterceptors(List<String> classes) {
        return String.join(",", classes);
    }
}