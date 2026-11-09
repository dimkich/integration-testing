package io.github.dimkich.integration.testing.collections.probe;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test fixture that invokes order-sensitive JDK collection factories. The class lives under
 * a package prefix declared by the test class annotation, so its bytecode is instrumented.
 * <p>
 * Each {@code *Report} method performs several related calls and returns the observed result as
 * a list of {@code name=value} tokens, so a single XML case verifies a whole contract area:
 * iteration order, wrapper class, mutability, and reactions to {@code null}/duplicates.
 * </p>
 * <p>
 * Several probes deliberately violate the JDK collection contracts (null arguments, duplicate
 * keys/elements, mutation of unmodifiable views). The corresponding IDE inspections are
 * suppressed on purpose: the violations are the subject under test, not defects.
 * </p>
 */
@Component
@SuppressWarnings({"OverwrittenKey", "DataFlowIssue", "ConstantValue"})
public class CollectionsProbe {

    public List<String> mapOfOrder(String k1, String k2, String k3) {
        return new ArrayList<>(Map.of(k1, 1, k2, 2, k3, 3).keySet());
    }

    public List<String> mapOfReport() {
        Map<String, Integer> single = Map.of("a", 1);
        Map<String, Integer> ten = Map.of(
                "k1", 1, "k2", 2, "k3", 3, "k4", 4, "k5", 5,
                "k6", 6, "k7", 7, "k8", 8, "k9", 9, "k10", 10);
        Map<String, Integer> entries = Map.ofEntries(
                Map.entry("c", 3),
                Map.entry("a", 1),
                Map.entry("b", 2));
        return new ArrayList<>(List.of(
                "class=" + single.getClass().getName(),
                "empty=" + Map.of().size(),
                "single=" + String.join(",", single.keySet()),
                "ten=" + String.join(",", ten.keySet()),
                "entries=" + String.join(",", entries.keySet())));
    }

    public String mapOfContract() {
        List<String> result = new ArrayList<>();
        result.add("nullKey=" + outcome(() -> Map.of(null, 1)));
        result.add("nullValue=" + outcome(() -> Map.of("a", null)));
        result.add("duplicate=" + outcome(() -> Map.of("a", 1, "a", 2)));
        result.add("unmodifiable=" + outcome(() -> Map.of("a", 1).put("b", 2)));
        return String.join(",", result);
    }

    public List<String> mapCopyOfReport() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("c", 3);
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Integer> withNull = new LinkedHashMap<>();
        withNull.put("a", null);

        Map<String, Integer> copy = Map.copyOf(source);
        List<String> result = new ArrayList<>(copy.keySet());
        result.add("class=" + copy.getClass().getName());
        result.add("unmodifiable=" + outcome(() -> Map.copyOf(source).put("d", 4)));
        result.add("nullValue=" + outcome(() -> Map.copyOf(withNull)));
        return result;
    }

    public List<String> setOfOrder(String e1, String e2, String e3) {
        return new ArrayList<>(Set.of(e1, e2, e3));
    }

    public List<String> setOfReport() {
        Set<String> single = Set.of("a");
        Set<String> ten = Set.of("k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9", "k10");
        Set<String> varargs = Set.of(
                "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9", "k10", "k11");
        return new ArrayList<>(List.of(
                "class=" + single.getClass().getName(),
                "empty=" + Set.of().size(),
                "single=" + String.join(",", single),
                "ten=" + String.join(",", ten),
                "varargs=" + String.join(",", varargs)));
    }

    public String setOfContract() {
        List<String> result = new ArrayList<>();
        result.add("null=" + outcome(() -> Set.of("a", null)));
        result.add("duplicate=" + outcome(() -> Set.of("a", "a")));
        result.add("unmodifiable=" + outcome(() -> Set.of("a").add("b")));
        return String.join(",", result);
    }

    public List<String> setCopyOfReport() {
        Set<String> source = new LinkedHashSet<>(List.of("c", "a", "b"));
        Set<String> withNull = new LinkedHashSet<>(List.of("a"));
        withNull.add(null);

        Set<String> copy = Set.copyOf(source);
        List<String> result = new ArrayList<>(copy);
        result.add("class=" + copy.getClass().getName());
        result.add("dedupe=" + String.join(",", Set.copyOf(List.of("b", "a", "b"))));
        result.add("unmodifiable=" + outcome(() -> Set.copyOf(source).add("d")));
        result.add("null=" + outcome(() -> Set.copyOf(withNull)));
        return result;
    }

    public List<String> toSetReport() {
        Set<String> set = Stream.of("c", "a", "b").collect(Collectors.toSet());
        List<String> result = new ArrayList<>(set);
        result.add("class=" + set.getClass().getName());
        result.add("mutable=" + set.add("d"));
        return result;
    }

    public List<String> toUnmodifiableSetReport() {
        Set<String> set = Stream.of("c", "a", "b").collect(Collectors.toUnmodifiableSet());
        List<String> result = new ArrayList<>(set);
        result.add("class=" + set.getClass().getName());
        result.add("unmodifiable=" + outcome(() -> set.add("d")));
        result.add("null=" + outcome(() -> Stream.of("a", null).collect(Collectors.toUnmodifiableSet())));
        return result;
    }

    public List<String> toMapReport() {
        Map<String, String> map = Stream.of("c1", "a1", "b1")
                .collect(Collectors.toMap(s -> s.substring(0, 1), s -> s));
        List<String> result = new ArrayList<>(map.keySet());
        result.add("class=" + map.getClass().getName());
        result.add("mutable=" + (map.put("d", "d1") == null));
        result.add("duplicate=" + outcome(() -> Stream.of("a1", "a2")
                .collect(Collectors.toMap(s -> s.substring(0, 1), s -> s))));
        return result;
    }

    public List<String> toUnmodifiableMapReport() {
        Map<String, String> map = Stream.of("c1", "a1", "b1")
                .collect(Collectors.toUnmodifiableMap(s -> s.substring(0, 1), s -> s));
        List<String> result = new ArrayList<>(map.keySet());
        result.add("class=" + map.getClass().getName());
        result.add("unmodifiable=" + outcome(() -> Stream.of("c1", "a1")
                .collect(Collectors.toUnmodifiableMap(s -> s.substring(0, 1), s -> s))
                .put("b", "b1")));
        result.add("nullKey=" + outcome(() -> Stream.of("a")
                .collect(Collectors.toUnmodifiableMap(s -> null, s -> s))));
        result.add("nullValue=" + outcome(() -> Stream.of("a")
                .collect(Collectors.toUnmodifiableMap(s -> s, s -> null))));
        return result;
    }

    public List<String> groupingByReport() {
        Map<String, List<String>> groups = Stream.of("b1", "a1", "b2")
                .collect(Collectors.groupingBy(s -> s.substring(0, 1)));
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            result.add(entry.getKey() + "=" + String.join(",", entry.getValue()));
        }
        Map<String, Long> downstream = Stream.of("b1", "a1", "b2")
                .collect(Collectors.groupingBy(s -> s.substring(0, 1), Collectors.counting()));
        result.add("downstream=" + String.join(",", downstream.keySet()));
        return result;
    }

    public String newHashMapClass() {
        Map<String, String> map = new HashMap<>();
        map.put("x", "y");
        return map.getClass().getName();
    }

    public List<String> hashMapReport() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("c", 3);
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Integer> plain = fill(new HashMap<>());
        Map<String, Integer> capacity = fill(new HashMap<>(16));
        Map<String, Integer> loadFactor = fill(new HashMap<>(16, 0.75f));
        return new ArrayList<>(List.of(
                "class=" + plain.getClass().getName(),
                "default=" + String.join(",", plain.keySet()),
                "capacity=" + String.join(",", capacity.keySet()),
                "loadFactor=" + String.join(",", loadFactor.keySet()),
                "copy=" + String.join(",", new HashMap<>(source).keySet())));
    }

    public List<String> hashMapNestedAndBranch(String value) {
        Map<String, Integer> nested = new HashMap<>(Map.of("c", 3, "a", 1, "b", 2));
        Map<String, Integer> branch;
        if ("x".equals(value)) {
            branch = new HashMap<>(4);
        } else {
            branch = new HashMap<>(8);
        }
        branch.put("c", 3);
        branch.put("a", 1);
        branch.put("b", 2);
        return new ArrayList<>(List.of(
                "nested=" + String.join(",", nested.keySet()),
                "branch=" + String.join(",", branch.keySet())));
    }

    public List<String> hashMapSubclasses() {
        Map<String, String> anonymous = new HashMap<>() {{
            put("x", "y");
        }};
        return new ArrayList<>(List.of(
                "subclassSize=" + new HashMapSubclass().size(),
                "anonymousSuperclass=" + anonymous.getClass().getSuperclass().getName()));
    }

    public String newHashSetClass() {
        Set<String> set = new HashSet<>();
        set.add("x");
        return set.getClass().getName();
    }

    public List<String> hashSetReport() {
        Set<String> source = new LinkedHashSet<>(List.of("c", "a", "b"));
        Set<String> plain = new HashSet<>();
        plain.add("c");
        plain.add("a");
        plain.add("b");
        Set<String> capacity = new HashSet<>(16);
        capacity.add("c");
        capacity.add("a");
        capacity.add("b");
        return new ArrayList<>(List.of(
                "class=" + plain.getClass().getName(),
                "default=" + String.join(",", plain),
                "capacity=" + String.join(",", capacity),
                "collection=" + String.join(",", new HashSet<>(source))));
    }

    private static Map<String, Integer> fill(Map<String, Integer> map) {
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        return map;
    }

    private static String outcome(Supplier<?> action) {
        try {
            action.get();
            return "no exception";
        } catch (RuntimeException e) {
            return e.getClass().getName();
        }
    }
}
