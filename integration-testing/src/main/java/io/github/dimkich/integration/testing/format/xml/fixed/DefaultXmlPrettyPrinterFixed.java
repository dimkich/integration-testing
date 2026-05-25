package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import lombok.SneakyThrows;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

public class DefaultXmlPrettyPrinterFixed extends DefaultXmlPrettyPrinter {
    private static final Field nextNameField;
    private static final QName emptyQName = new QName("");
    private final Deque<Boolean> tagWasWrittenStack = new ArrayDeque<>();

    static {
        try {
            nextNameField = ToXmlGenerator.class.getDeclaredField("_nextName");
            nextNameField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultXmlPrettyPrinterFixed() {
    }

    public DefaultXmlPrettyPrinterFixed(DefaultXmlPrettyPrinter base) {
        super(base);
    }

    @Override
    @SneakyThrows
    public void writeStartObject(JsonGenerator gen) {
        if (emptyQName.equals(nextNameField.get(gen))) {
            tagWasWrittenStack.push(false);
            return;
        }
        tagWasWrittenStack.push(true);
        super.writeStartObject(gen);
    }

    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
        if (!tagWasWrittenStack.pop()) {
            return;
        }
        super.writeEndObject(gen, nrOfEntries);
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
