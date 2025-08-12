package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.*;

import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
public class KeyValueStorageInit extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean clear;
    private Map<String, Object> map;

    @RequiredArgsConstructor
    public static class Init implements Initializer<KeyValueStorageInit> {
        private final TestDataStorages testDataStorages;
        private final Map<String, InitState> storageInitState = new HashMap<>();

        @Override
        public Class<KeyValueStorageInit> getTestInitClass() {
            return KeyValueStorageInit.class;
        }

        @Override
        public Integer getOrder() {
            return 3000;
        }

        @Override
        public void init(Stream<KeyValueStorageInit> inits) throws Exception {
            storageInitState.forEach((k, v) -> v.clear());
            inits.forEach(init -> {
                InitState initState = storageInitState.computeIfAbsent(init.getName(), k -> new InitState());
                if (init.getClear() != null && init.getClear()) {
                    initState.clear = true;
                    initState.map.clear();
                }
                if (init.getMap() != null) {
                    initState.map.putAll(init.getMap());
                }
            });
            for (Map.Entry<String, InitState> entry : storageInitState.entrySet()) {
                KeyValueDataStorage storage = testDataStorages.getTestDataStorage(entry.getKey(),
                        KeyValueDataStorage.class);
                if (entry.getValue().clear) {
                    storage.clearAll();
                    testDataStorages.addAffectedStorage(storage);
                }
                if (!entry.getValue().map.isEmpty()) {
                    storage.putKeysData(entry.getValue().map);
                    testDataStorages.addAffectedStorage(storage);
                }
            }
        }
    }

    public static class InitState {
        private final Map<String, Object> map = new LinkedHashMap<>();
        private boolean clear;

        public void clear() {
            clear = false;
            map.clear();
        }
    }
}
