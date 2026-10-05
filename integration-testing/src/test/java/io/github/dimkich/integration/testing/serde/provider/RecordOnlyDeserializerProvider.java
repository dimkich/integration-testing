package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class RecordOnlyDeserializerProvider
        implements TestSerdeProviderFactory<byte[], Object, TestSerdeContext, ComponentRole,
        TestRecordProperties> {

    private final Class<byte[]> inputClass = byte[].class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<TestRecordProperties> propertiesClass = TestRecordProperties.class;
    private final String name = "record-only";

    @Override
    public TestSerdeConverter<byte[], Object, TestSerdeContext> create(TestRecordProperties config,
                                                                       Class<byte[]> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new TaggedDeserializer("ONLY");
    }
}
