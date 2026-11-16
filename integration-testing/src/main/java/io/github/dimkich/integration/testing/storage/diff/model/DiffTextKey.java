package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;

/**
 * Text key of a difference element: a record identifier, a field name or a map key rendered as a
 * single line.
 * <p>
 * Text keys are written as the {@code key} attribute in XML and as a plain string in JSON, so
 * existing expected data keeps its shape.
 *
 * @param text identifier of the element, never {@code null}
 */
public record DiffTextKey(String text) implements DiffKey {

    public DiffTextKey {
        if (text == null) {
            throw new IllegalArgumentException("Text key must not be null");
        }
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public int compareContent(DiffKey other) {
        return text.compareTo(((DiffTextKey) other).text);
    }

    @Override
    public String description() {
        return "text key '" + text + "'";
    }

    @Override
    public void write(DiffWriter writer) throws IOException {
        writer.writeKey(this);
    }
}
