package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class RecordOnlySerializerProvider
        implements TestSerdeProviderFactory<Object, byte[], TestSerdeContext, ComponentRole,
        TestRecordProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<TestRecordProperties> propertiesClass = TestRecordProperties.class;
    private final String name = "record-only";

    @Override
    public TestSerdeConverter<Object, byte[], TestSerdeContext> create(TestRecordProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<byte[]> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new TaggedSerializer("ONLY");
    }
}
