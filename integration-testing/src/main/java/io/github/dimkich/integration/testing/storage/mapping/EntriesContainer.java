package io.github.dimkich.integration.testing.storage.mapping;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

@Data
public abstract class EntriesContainer<T extends Comparable<T>> implements Container {
    protected Collection<T> entry = new ArrayList<>();

    @Override
    public void setSort(Boolean sort) {
        if (sort) {
            entry = new TreeSet<>();
        } else {
            entry = new ArrayList<>();
        }
    }

    @Override
    public boolean isEmpty() {
        return entry.isEmpty();
    }
}