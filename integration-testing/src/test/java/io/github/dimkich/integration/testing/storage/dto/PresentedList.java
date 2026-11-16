package io.github.dimkich.integration.testing.storage.dto;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List with its own presentation: a body of another value may be this list, and the engine has
 * to apply the list presenter instead of rendering the list as plain collection elements.
 */
public class PresentedList extends ArrayList<String> {

    public PresentedList() {
    }

    public PresentedList(Collection<String> values) {
        super(values);
    }
}
