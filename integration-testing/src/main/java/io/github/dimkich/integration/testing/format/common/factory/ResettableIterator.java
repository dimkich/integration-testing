package io.github.dimkich.integration.testing.format.common.factory;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Iterator wrapper that can restart iteration from the beginning of the backing collection.
 *
 * @param <T> element type
 */
public class ResettableIterator<T> implements Iterator<T> {
    @Getter(onMethod_ = @JsonValue)
    private final Collection<T> collection;
    private Iterator<T> iterator;

    /**
     * Creates a new resettable iterator for the provided collection.
     *
     * @param collection source collection to iterate
     */
    public ResettableIterator(Collection<T> collection) {
        this.collection = collection;
        this.iterator = collection.iterator();
    }

    /**
     * Resets this iterator to the beginning of the underlying collection.
     */
    public void reset() {
        iterator = collection.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        iterator.forEachRemaining(action);
    }
}
