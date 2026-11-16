package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.PresentedList;

/**
 * Uses the list itself as the key body. The key and value presenters of one type are looked up
 * separately, so the key keeps the list while the value presenter of the same type describes its
 * size.
 */
public class PresentedListKeyPresenter implements DiffPresenter<PresentedList> {

    @Override
    public Object present(PresentedList value, DiffAttributes.Builder attributes) {
        return value;
    }
}
