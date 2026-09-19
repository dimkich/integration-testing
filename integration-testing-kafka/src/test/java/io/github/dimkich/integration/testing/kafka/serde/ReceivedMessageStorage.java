package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component("receivedMessages")
public class ReceivedMessageStorage implements TestDataStorage {

    private final AtomicLong counter = new AtomicLong();
    private final Map<String, KafkaRecord> messages = new LinkedHashMap<>();

    public void put(KafkaRecord record) {
        messages.put("msg-" + counter.getAndIncrement(), record);
    }

    public void storeReceived(String topic, String key, Object payload, Map<String, Object> headers) {
        KafkaRecord record = new KafkaRecord();
        record.setTopic(topic);
        record.setKey(key);
        record.setValue(payload);
        MultiValueMap<String, Object> cleanHeaders = new LinkedMultiValueMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (!k.startsWith("kafka_") && !"id".equals(k) && !"timestamp".equals(k)) {
                    if (v instanceof List<?> list) {
                        for (Object item : list) {
                            cleanHeaders.add(k, item);
                        }
                    } else {
                        cleanHeaders.add(k, v);
                    }
                }
            });
        }
        record.setHeaders(cleanHeaders);
        put(record);
    }

    @Override
    public String getName() {
        return "receivedMessages";
    }

    @Override
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return new LinkedHashMap<>(messages);
    }

    @Override
    public void setDiff(Map<String, Object> diff) {
        messages.clear();
        counter.set(0);
    }
}
