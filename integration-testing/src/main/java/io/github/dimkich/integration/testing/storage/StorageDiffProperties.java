package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.storage.mapping.Container;
import lombok.Data;

import java.util.List;

@Data
public class StorageDiffProperties {
    private Container.Type defaultKeyType;
    private List<Container.Type> keyType;
    private Container.Type defaultValueType;
    private List<Container.Type> valueType;
    private Boolean defaultSort;
    private List<Boolean> sort;
    private Boolean defaultChangeType;
    private List<Boolean> changeType;

    public Container.Type getKeyType(int level) {
        return getFromList(level, keyType, defaultKeyType);
    }

    public Container.Type getValueType(int level) {
        return getFromList(level, valueType, defaultValueType);
    }

    public Boolean getSort(int level) {
        return getFromList(level, sort, defaultSort);
    }

    public Boolean getChangeType(int level) {
        return getFromList(level, changeType, defaultChangeType);
    }

    private <T> T getFromList(int i, List<T> list, T defaultValue) {
        if (list == null || list.size() <= i || list.get(i) == null) {
            return defaultValue;
        }
        return list.get(i);
    }
}
