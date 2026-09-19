package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.BetaPlatformSerializer;

import java.nio.charset.StandardCharsets;

public class AmbiguousBetaAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        BetaPlatformSerializer,
        SerdeProperties> {

    @Override
    public BetaPlatformSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                        SerdeProperties properties) {
        return data -> ("B0:" + data).getBytes(StandardCharsets.UTF_8);
    }
}
