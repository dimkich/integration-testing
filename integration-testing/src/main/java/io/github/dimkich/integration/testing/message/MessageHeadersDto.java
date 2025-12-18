package io.github.dimkich.integration.testing.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * DTO that represents message headers as a sorted map.
 * <p>
 * The class is designed to be used with Jackson. All unknown JSON
 * properties are stored in the internal {@code headers} map via
 * {@link #put(String, Object)} and exposed via {@link #getAttributes()}.
 * </p>
 * <p>
 * There are also convenience accessors for commonly used header keys:
 * {@link #TOPIC}, {@link #SOURCE} and {@link #KEY}.
 * </p>
 */
@ToString
public class MessageHeadersDto {

    /**
     * Standard header name for the message topic.
     */
    public static final String TOPIC = "topic";

    /**
     * Standard header name for the message source system.
     */
    public static final String SOURCE = "source";

    /**
     * Standard header name for the message key.
     */
    public static final String KEY = "key";

    /**
     * Internal storage for all headers.
     * <p>
     * A {@link TreeMap} is used to keep keys sorted and ensure
     * deterministic order, which is helpful for testing and logging.
     * </p>
     */
    private final Map<String, Object> headers = new TreeMap<>();

    /**
     * Returns all header attributes.
     *
     * @return unmodifiable view of the internal headers map
     */
    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return headers;
    }

    /**
     * Adds or replaces a header value.
     * <p>
     * This method is used by Jackson to capture arbitrary JSON properties.
     * </p>
     *
     * @param name  header name, must not be {@code null}
     * @param value header value, may be {@code null}
     */
    @JsonAnySetter
    public void put(String name, Object value) {
        headers.put(name, value);
    }

    /**
     * Removes a header by name.
     *
     * @param name header name to remove
     */
    public void remove(String name) {
        headers.remove(name);
    }

    /**
     * Returns a mutable set view of the headers.
     *
     * @return entry set of the internal headers map
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return headers.entrySet();
    }

    /**
     * Returns the value of the {@link #TOPIC} header.
     *
     * @return topic header value or {@code null} if not present
     */
    @JsonIgnore
    public Object getTopic() {
        return headers.get(TOPIC);
    }

    /**
     * Sets the {@link #TOPIC} header.
     *
     * @param topic topic value to set
     */
    public void setTopic(Object topic) {
        headers.put(TOPIC, topic);
    }

    /**
     * Returns the value of the {@link #SOURCE} header.
     *
     * @return source header value or {@code null} if not present
     */
    @JsonIgnore
    public Object getSource() {
        return headers.get(SOURCE);
    }

    /**
     * Sets the {@link #SOURCE} header.
     *
     * @param source source value to set
     */
    public void setSource(Object source) {
        headers.put(SOURCE, source);
    }

    /**
     * Returns the value of the {@link #KEY} header.
     *
     * @return key header value or {@code null} if not present
     */
    @JsonIgnore
    public Object getKey() {
        return headers.get(KEY);
    }

    /**
     * Sets the {@link #KEY} header.
     *
     * @param key key value to set
     */
    public void setKey(Object key) {
        headers.put(KEY, key);
    }
}
