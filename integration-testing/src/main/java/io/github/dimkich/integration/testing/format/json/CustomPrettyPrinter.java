package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

public class CustomPrettyPrinter extends DefaultPrettyPrinter {
    public CustomPrettyPrinter() {
        this._objectIndenter = new DefaultIndenter("  ", "\n");
        this._arrayIndenter = new DefaultIndenter("  ", "\n");
    }

    public CustomPrettyPrinter(DefaultPrettyPrinter base) {
        super(base);
        this._objectIndenter = new DefaultIndenter("  ", "\n");
        this._arrayIndenter = new DefaultIndenter("  ", "\n");
    }

    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
        g.writeRaw(": ");
    }

    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!this._arrayIndenter.isInline()) {
            this._nesting--;
        }

        if (nrOfValues > 0) {
            this._arrayIndenter.writeIndentation(g, this._nesting);
        }

        g.writeRaw(']');
    }

    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!this._objectIndenter.isInline()) {
            this._nesting--;
        }

        if (nrOfEntries > 0) {
            this._objectIndenter.writeIndentation(g, this._nesting);
        }

        g.writeRaw('}');
    }

    public CustomPrettyPrinter createInstance() {
        return new CustomPrettyPrinter(this);
    }
}
