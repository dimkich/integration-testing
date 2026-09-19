package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.AlphaPlatformSerializer;

import java.nio.charset.StandardCharsets;

public class AmbiguousAlphaAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        AlphaPlatformSerializer,
        SerdeProperties> {

    @Override
    public AlphaPlatformSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                         SerdeProperties properties) {
        return data -> ("A:" + data).getBytes(StandardCharsets.UTF_8);
    }
}
