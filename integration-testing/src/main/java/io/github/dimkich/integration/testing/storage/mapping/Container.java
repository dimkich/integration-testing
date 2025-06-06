package io.github.dimkich.integration.testing.storage.mapping;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.function.Function;

public interface Container {
    enum ChangeType {added, changed, deleted}

    enum Type {STRING, OBJECT}

    static Container create(Type keyType, Type valueType, Boolean sort, Boolean changeType) {
        Container container = switch (keyType) {
            case STRING -> switch (valueType) {
                case STRING -> changeType ? error(keyType, valueType, changeType) : new MapStringKeyStringValue();
                case OBJECT -> changeType ? new EntriesStringKeyObjectValue() : new MapStringKeyObjectValue();
            };
            case OBJECT -> switch (valueType) {
                case STRING -> error(keyType, valueType, changeType);
                case OBJECT -> changeType ? new EntriesObjectKeyObjectValue() : error(keyType, valueType, changeType);
            };
        };
        container.setSort(sort);
        return container;
    }

    private static Container error(Type keyType, Type valueType, Boolean changeType) {
        throw new RuntimeException(String.format("Container with keyType = %s, valueType = %s, changeType = %s not supported",
                keyType, valueType, changeType));
    }

    void setSort(Boolean sort);

    void addEntry(ChangeType change, Object key, Object value, Function<Object, String> toString);

    @JsonIgnore
    boolean isEmpty();
}
