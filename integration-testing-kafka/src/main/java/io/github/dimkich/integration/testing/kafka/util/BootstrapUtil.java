package io.github.dimkich.integration.testing.kafka.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Canonicalizes {@code bootstrap.servers} strings into a stable key used to
 * bucket per-cluster state (see {@code InFlightLedger}).
 *
 * <p>The result is <b>memoized per raw input</b>, so every code path derives the
 * same key for the lifetime of the JVM. Interceptors (producer send/commit),
 * the state checker and bean registration all normalize the same raw value; without
 * the cache a multi-address host (round-robin / load-balanced DNS) could resolve
 * to different IPs at different times, so end offsets would be written into one
 * bucket while the state checker reads another &mdash; a false {@code no lag}/{@code no-state}.
 *
 * <p>The cache also removes a DNS lookup from the per-message hot path
 * ({@code onSend}/{@code onCommit}/{@code recordEndOffset}): the lookup happens
 * exactly once per unique bootstrap string, after which normalization is a
 * {@link ConcurrentHashMap} lookup.
 *
 * <p>Hosts are resolved with {@link InetAddress#getAllByName(String)} and the addresses
 * are sorted and deduplicated, so a multi-IP host always yields the same
 * deterministic key regardless of resolver order.
 */
public final class BootstrapUtil {

    private static final ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<>();

    private BootstrapUtil() {
    }

    /**
     * Returns the memoized canonical key for the given {@code bootstrap.servers} value.
     *
     * @param bs the raw bootstrap servers value, may be {@code null}
     * @return the canonical key, or {@code null} if {@code bs} is {@code null}
     */
    public static String normalize(String bs) {
        if (bs == null) {
            return null;
        }
        return CACHE.computeIfAbsent(bs, BootstrapUtil::normalizeUncached);
    }

    /**
     * Converts a client config {@code bootstrap.servers} value into its
     * comma-separated string form.
     *
     * <p>Kafka accepts the setting as either a {@code String} or a
     * {@code List<String>}, and hands the raw value to interceptors and
     * construction advice unchanged. Spring Boot's
     * {@code KafkaProperties.bootstrapServers} is a {@code List<String>}, so both
     * shapes reach this code; a plain cast to {@code String} would fail with a
     * {@link ClassCastException} for the list form.
     */
    public static String asString(Object servers) {
        if (servers == null) {
            return null;
        }
        if (servers instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(servers);
    }

    private static String normalizeUncached(String bs) {
        try {
            return Arrays.stream(bs.split(","))
                    .map(String::trim)
                    .map(BootstrapUtil::resolveAddress)
                    .sorted()
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            return bs.replaceAll("\\s+", "");
        }
    }

    /**
     * Resolves a single {@code host:port} or bracketed {@code [host]:port} entry to a
     * deterministic {@code ip1|ip2:port} form. IPv6 literals arrive bracketed
     * ({@code [::1]:9092}); the brackets are stripped before resolving, since
     * {@code getAllByName("[::1]")} would throw {@code UnknownHostException} and return
     * the raw literal, yielding a key inconsistent with an equivalent hostname entry.
     */
    private static String resolveAddress(String addr) {
        String host;
        String port;
        if (addr.startsWith("[")) {
            int close = addr.indexOf(']');
            if (close < 0) {
                return addr;
            }
            host = addr.substring(1, close);
            String rest = addr.substring(close + 1);
            port = rest.startsWith(":") ? rest.substring(1) : "";
        } else {
            int colon = addr.lastIndexOf(':');
            if (colon <= 0) {
                return addr;
            }
            host = addr.substring(0, colon);
            port = addr.substring(colon + 1);
        }
        try {
            String ips = Arrays.stream(InetAddress.getAllByName(host))
                    .map(InetAddress::getHostAddress)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("|"));
            return port.isEmpty() ? ips : ips + ":" + port;
        } catch (UnknownHostException e) {
            return addr;
        }
    }
}