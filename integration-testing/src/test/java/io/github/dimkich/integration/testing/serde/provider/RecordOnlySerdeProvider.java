package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class RecordOnlySerdeProvider implements TestSerdeProvider<TestRecordProperties> {
    @Override
    public String getName() {
        return "record-only";
    }

    @Override
    public Object createSerializer(TestRecordProperties config) {
        return new TaggedSerializer("ONLY");
    }

    @Override
    public Object createDeserializer(TestRecordProperties config) {
        return new TaggedDeserializer("ONLY");
    }
}
