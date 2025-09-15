package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import lombok.NoArgsConstructor;

import javax.xml.namespace.QName;
import java.io.IOException;

@NoArgsConstructor
public class DefaultXmlPrettyPrinterFixed extends DefaultXmlPrettyPrinter {
    public DefaultXmlPrettyPrinterFixed(DefaultXmlPrettyPrinter base) {
        super(base);
    }

    @Override
    public void writeStartArray(JsonGenerator gen) throws IOException {
        if (gen.getOutputContext().getParent().inArray()) {
            String wrapperName = JacksonUtils.getCurrentName(gen);
            ToXmlGenerator generator = (ToXmlGenerator) gen;
            generator.startWrappedValue(new QName(wrapperName), new QName(wrapperName));
        }
    }

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
        if (gen.getOutputContext().getParent().inArray()) {
            ToXmlGenerator generator = (ToXmlGenerator) gen;
            generator.finishWrappedValue(new QName(""), null);
        }
    }

    @Override
    public DefaultXmlPrettyPrinter createInstance() {
        return new DefaultXmlPrettyPrinterFixed(this);
    }
}
