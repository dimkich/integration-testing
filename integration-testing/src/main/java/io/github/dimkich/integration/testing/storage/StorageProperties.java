package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.storage.mapping.Container;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "integration.testing.storage")
public class StorageProperties extends StorageDiffProperties {
    private final static StoragesProperties defaultProperty = new StoragesProperties();
    private final static Set<String> mergedExcludedFields = new HashSet<>();
    private boolean enabled;
    private Map<String, StoragesProperties> storages = new HashMap<>();

    {
        setDefaultKeyType(Container.Type.STRING);
        setDefaultValueType(Container.Type.OBJECT);
        setDefaultSort(false);
        setDefaultChangeType(false);
    }

    public Container.Type getKeyType(String name, int level) {
        return getConfigValue(name, level, StorageDiffProperties::getKeyType);
    }

    public Container.Type getValueType(String name, int level) {
        return getConfigValue(name, level, StorageDiffProperties::getValueType);
    }

    public Boolean getSort(String name, int level) {
        return getConfigValue(name, level, StorageDiffProperties::getSort);
    }

    public Boolean getChangeType(String name, int level) {
        return getConfigValue(name, level, StorageDiffProperties::getChangeType);
    }

    public Map<String, Set<String>> getExcludedFields(String name) {
        StoragesProperties properties = storages.get(name);
        if (properties == null || properties.getExcludedFields() == null) {
            return getExcludedFields() == null ? Map.of() : getExcludedFields();
        } else if (getExcludedFields() == null) {
            return properties.getExcludedFields();
        }
        if (!mergedExcludedFields.contains(name)) {
            for (Map.Entry<String, Set<String>> entry : getExcludedFields().entrySet()) {
                properties.getExcludedFields().merge(entry.getKey(), entry.getValue(), (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                });
            }
            mergedExcludedFields.add(name);
        }
        return properties.getExcludedFields();
    }

    private <T> T getConfigValue(String name, int level, BiFunction<StorageDiffProperties, Integer, T> get) {
        T result = null;
        StoragesProperties properties = storages.get(name);
        if (properties != null) {
            result = get.apply(properties, level);
        }
        return result == null ? get.apply(this, level) : result;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class StoragesProperties extends StorageDiffProperties {
        private String url;
        private String username;
        private String password;
    }
}
