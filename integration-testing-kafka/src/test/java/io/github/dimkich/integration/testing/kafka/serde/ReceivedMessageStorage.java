package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.storage.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.SnapshotTestDataStorage;
import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component("receivedMessages")
public class ReceivedMessageStorage extends SnapshotTestDataStorage {

    private final AtomicLong counter = new AtomicLong();
    private final Map<String, KafkaRecord> messages = new LinkedHashMap<>();

    public ReceivedMessageStorage(ObjectsDifference objectsDifference) {
        super(objectsDifference);
    }

    public void put(KafkaRecord record) {
        messages.put("msg-" + counter.getAndIncrement(), record);
        markAffected();
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
    protected Map<String, Object> doGetCurrentValue() {
        return new LinkedHashMap<>(messages);
    }

    @Override
    public Object getDiff() throws Exception {
        Object diff = super.getDiff();
        boolean hadMessages = !messages.isEmpty();
        messages.clear();
        counter.set(0);
        if (hadMessages) {
            markAffected();
        }
        return diff;
    }
}
