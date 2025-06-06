package io.github.dimkich.integration.testing.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@ToString
public class MessageHeadersDto {
    public final static String TOPIC = "topic";
    public final static String SOURCE = "source";
    public final static String KEY = "key";
    private final Map<String, Object> headers = new TreeMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return headers;
    }

    @JsonAnySetter
    public void put(String name, Object value) {
        headers.put(name, value);
    }

    public void remove(String name) {
        headers.remove(name);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return headers.entrySet();
    }

    @JsonIgnore
    public Object getTopic() {
        return headers.get(TOPIC);
    }

    public void setTopic(Object topic) {
        headers.put(TOPIC, topic);
    }

    @JsonIgnore
    public Object getSource() {
        return headers.get(SOURCE);
    }

    public void setSource(Object source) {
        headers.put(SOURCE, source);
    }

    @JsonIgnore
    public Object getKey() {
        return headers.get(KEY);
    }

    public void setKey(Object key) {
        headers.put(KEY, key);
    }
}
