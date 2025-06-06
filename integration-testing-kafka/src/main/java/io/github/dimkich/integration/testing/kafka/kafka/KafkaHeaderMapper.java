package io.github.dimkich.integration.testing.kafka.kafka;

import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;

public class KafkaHeaderMapper extends DefaultKafkaHeaderMapper {
    @Override
    protected Object headerValueToAddIn(Header header) {
        if (header instanceof HeaderWithObject headerWithObject) {
            return headerWithObject.object();
        }
        return super.headerValueToAddIn(header);
    }
}
