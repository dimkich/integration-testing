package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;

/**
 * Identifier of a difference element.
 * <p>
 * A key is either a single line of text ({@link DiffTextKey}) or a structure ({@link DiffObjectKey}). The
 * form is chosen by the type of the key: a type registered as an object key carries its own
 * structure, everything else is rendered as text. The key identifies an element inside its parent
 * and is never read back into a domain object.
 * <p>
 * A key is also its own identity: the record implementations compare by content, so the same
 * rendered structure matches before and after the test. The natural order is deterministic and is
 * used only for output order: object keys go before text keys, text keys compare by text, object
 * keys compare by type, attributes and body. The order is consistent with {@code equals}.
 *
 * @see DiffTextKey
 * @see DiffObjectKey
 */
public sealed interface DiffKey extends Comparable<DiffKey> permits DiffTextKey, DiffObjectKey {

    /**
     * @return position of the form in the natural order: an object key goes before a text key
     */
    int rank();

    /**
     * Compares the content with another key of the same form.
     *
     * @param other key with the same {@link #rank()}
     * @return comparison result
     */
    int compareContent(DiffKey other);

    /**
     * Describes the form of the key for diagnostics, for example an error message.
     *
     * @return description of the key form
     */
    String description();

    /**
     * Writes the key into the given sink.
     *
     * @param writer sink of the key
     */
    void write(DiffWriter writer) throws IOException;

    @Override
    default int compareTo(DiffKey other) {
        int order = Integer.compare(rank(), other.rank());
        return order != 0 ? order : compareContent(other);
    }
}
