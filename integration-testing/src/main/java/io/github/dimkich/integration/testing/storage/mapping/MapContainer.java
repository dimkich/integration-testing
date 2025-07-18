package io.github.dimkich.integration.testing.storage.mapping;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public abstract class MapContainer<K, T> implements Container {
    @Getter(onMethod_ = {@JsonAnyGetter, @JsonInclude(JsonInclude.Include.ALWAYS)})
    protected Map<K, T> map = new LinkedHashMap<>();

    @JsonAnySetter
    public void put(K name, T value) {
        map.put(name, value);
    }

    @Override
    public void setSort(Boolean sort) {
        if (sort) {
            map = new TreeMap<>();
        } else {
            map = new LinkedHashMap<>();
        }
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clearNullValueKeys() {
        map.entrySet().removeIf(entry -> entry.getValue() == null);
    }
}
