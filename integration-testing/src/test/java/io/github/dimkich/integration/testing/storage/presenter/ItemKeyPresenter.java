package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.Item;

/**
 * Text key of an item: the name and the count joined by a comma.
 */
public class ItemKeyPresenter implements DiffPresenter<Item> {

    @Override
    public Object present(Item item, DiffAttributes.Builder attributes) {
        return item.getName() + "," + item.getCount();
    }
}
