package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

/**
 * Scalar value of a difference entry.
 * <p>
 * The value is kept as text: the XML format writes it verbatim, and the meaning of the text is
 * defined by the {@code type} attribute of the owning {@link DiffEntry}.
 *
 * @param text value rendered as text, never {@code null}
 */
public record DiffScalarValue(String text) implements DiffValue {
    private static final Comparator<String> TEXTS =
            Comparator.nullsFirst(Comparator.naturalOrder());

    public DiffScalarValue {
        Objects.requireNonNull(text, "text");
    }

    @Override
    public int rank() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int compareContent(DiffValue other) {
        return TEXTS.compare(text, ((DiffScalarValue) other).text());
    }

    /**
     * Writes the text of the value.
     */
    @Override
    public void write(DiffWriter writer) throws IOException {
        writer.writeText(text);
    }
}
