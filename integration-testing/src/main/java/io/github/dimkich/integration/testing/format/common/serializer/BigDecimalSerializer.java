package io.github.dimkich.integration.testing.format.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalSerializer extends StdScalarSerializer<BigDecimal> {
    public BigDecimalSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(bigDecimal.stripTrailingZeros().toPlainString());
    }
}
