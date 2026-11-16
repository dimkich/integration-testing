package io.github.dimkich.integration.testing.storage.diff.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names of the difference XML: element tags and metadata attributes.
 * <p>
 * The format has no namespaces, so a name with a colon is not usable. The reserved vocabularies
 * differ: {@code insert}, {@code update}, {@code delete} and {@code xmlns} cannot be element tags,
 * while {@code key}, {@code type}, {@code nil} and {@code xmlns} cannot be metadata attributes.
 * <p>
 * Both questions are pure functions of the name, so the answers are memoized in bounded maps shared
 * by the whole JVM. The test format does not affect an answer, so XML and JSON can run in the same
 * context. The caches are bounded: data driven names are unique and stop being cached after the
 * limit instead of growing the cache without bound.
 */
public final class XmlNames {
    /**
     * Upper bound of both caches.
     */
    private static final int CACHE_LIMIT = 100_000;

    private static final Map<String, Boolean> TAGS = new ConcurrentHashMap<>();
    private static final Map<String, Status> ATTRIBUTES = new ConcurrentHashMap<>();

    private XmlNames() {
    }

    /**
     * @param name element identifier
     * @return {@code true} when the identifier can be used as an XML tag
     */
    public static boolean isTag(String name) {
        if (name == null) {
            return false;
        }
        Boolean cached = TAGS.get(name);
        if (cached != null) {
            return cached;
        }
        boolean tag = isValid(name) && DiffOperation.fromTag(name) == null && !"xmlns".equals(name);
        if (TAGS.size() < CACHE_LIMIT) {
            TAGS.putIfAbsent(name, tag);
        }
        return tag;
    }

    /**
     * Validates one metadata attribute.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    static void validateAttribute(String name, String value) {
        Status status = status(name);
        if (status == Status.INVALID) {
            throw new IllegalArgumentException("Attribute name '" + name + "' is not a valid XML name");
        }
        if (status == Status.RESERVED) {
            throw new IllegalArgumentException("Attribute '" + name
                    + "' is reserved by the difference format; choose another name in DiffPresenter");
        }
        if (value == null) {
            throw new IllegalArgumentException("Attribute '" + name + "' must have a value");
        }
    }

    /**
     * Validates the names and values of the metadata attributes of one element. Duplicate names
     * are impossible: a map holds one value per name.
     *
     * @param attributes attributes to validate, may be {@code null}
     */
    static void validateAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            return;
        }
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            validateAttribute(attribute.getKey(), attribute.getValue());
        }
    }

    /**
     * @param name attribute name, may be {@code null}
     * @return rule matched by the attribute name
     */
    private static Status status(String name) {
        if (name == null) {
            return Status.INVALID;
        }
        Status cached = ATTRIBUTES.get(name);
        if (cached != null) {
            return cached;
        }
        Status status;
        if (!isValid(name)) {
            status = Status.INVALID;
        } else if (isReservedAttribute(name)) {
            status = Status.RESERVED;
        } else {
            status = Status.VALID;
        }
        if (ATTRIBUTES.size() < CACHE_LIMIT) {
            ATTRIBUTES.putIfAbsent(name, status);
        }
        return status;
    }

    /**
     * @param name attribute name
     * @return {@code true} when the name is reserved by the difference format
     */
    private static boolean isReservedAttribute(String name) {
        return "key".equals(name) || "type".equals(name) || "nil".equals(name) || "xmlns".equals(name);
    }

    /**
     * @param name possible XML name
     * @return {@code true} when the name can be used as an element or attribute name
     */
    private static boolean isValid(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        if (first < 128) {
            if (!(first == '_' || (first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z'))) {
                return false;
            }
        } else if (!Character.isLetter(first)) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 128) {
                if (!(c == '_' || c == '.' || c == '-'
                        || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
                    return false;
                }
            } else if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private enum Status {
        /**
         * Usable as a metadata attribute name.
         */
        VALID,
        /**
         * Not a valid XML name.
         */
        INVALID,
        /**
         * An attribute reserved by the difference format.
         */
        RESERVED
    }
}
