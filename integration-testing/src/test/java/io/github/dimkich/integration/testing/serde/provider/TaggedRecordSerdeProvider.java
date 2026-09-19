package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class TaggedRecordSerdeProvider implements TestSerdeProvider<TestRecordProperties> {
    @Override
    public String getName() {
        return "tagged";
    }

    @Override
    public Object createSerializer(TestRecordProperties config) {
        return new TaggedSerializer("REC");
    }

    @Override
    public Object createDeserializer(TestRecordProperties config) {
        return new TaggedDeserializer("REC");
    }
}
