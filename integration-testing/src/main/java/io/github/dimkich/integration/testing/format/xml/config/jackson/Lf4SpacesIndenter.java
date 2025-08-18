package io.github.dimkich.integration.testing.format.xml.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Serial;
import java.util.Arrays;

public class Lf4SpacesIndenter implements DefaultXmlPrettyPrinter.Indenter, java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    final static String SYSTEM_LINE_SEPARATOR = System.lineSeparator();

    final static int SPACE_COUNT = 64;
    final static char[] SPACES = new char[SPACE_COUNT];

    static {
        Arrays.fill(SPACES, ' ');
    }

    public Lf4SpacesIndenter() {
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public void writeIndentation(XMLStreamWriter2 sw, int level) throws XMLStreamException {
        sw.writeRaw(SYSTEM_LINE_SEPARATOR);
        level = 4 * level; // 2 spaces per level
        while (level > SPACE_COUNT) { // should never happen but...
            sw.writeRaw(SPACES, 0, SPACE_COUNT);
            level -= SPACES.length;
        }
        sw.writeRaw(SPACES, 0, level);
    }

    @Override
    public void writeIndentation(JsonGenerator jg, int level) throws IOException {
        jg.writeRaw(SYSTEM_LINE_SEPARATOR);
        level = 4 * level; // 2 spaces per level
        while (level > SPACE_COUNT) { // should never happen but...
            jg.writeRaw(SPACES, 0, SPACE_COUNT);
            level -= SPACES.length;
        }
        jg.writeRaw(SPACES, 0, level);
    }
}
