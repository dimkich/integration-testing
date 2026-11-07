package io.github.dimkich.integration.testing.storage.sql.table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * Mutable set of tables backed by the same dense bitset as the planner. The set is bound to a
 * {@link TableRegistry} so iteration can expose domain identities instead of raw ids.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TableSet extends AbstractSet<Table> {
    @Getter
    private final TableRegistry registry;
    private final BitSet bits;

    public static TableSet empty(TableRegistry registry) {
        return new TableSet(registry, new BitSet());
    }

    @Override
    public boolean add(Table table) {
        int id = table.getId();
        if (bits.get(id)) {
            return false;
        }
        bits.set(id);
        return true;
    }

    @Override
    public boolean remove(Object object) {
        if (!(object instanceof Table table)) {
            return false;
        }
        int id = table.getId();
        if (!bits.get(id)) {
            return false;
        }
        bits.clear(id);
        return true;
    }

    @Override
    public boolean contains(Object object) {
        if (!(object instanceof Table table)) {
            return false;
        }
        return bits.get(table.getId());
    }

    @Override
    public int size() {
        return bits.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    @Override
    public void clear() {
        bits.clear();
    }

    public TableSet copy() {
        return new TableSet(registry, (BitSet) bits.clone());
    }

    public void forEachTable(Consumer<Table> action) {
        for (int index = bits.nextSetBit(0); index >= 0; index = bits.nextSetBit(index + 1)) {
            action.accept(registry.get(index));
        }
    }

    @Override
    @NonNull
    public Iterator<Table> iterator() {
        return new Iterator<>() {
            private int next = bits.nextSetBit(0);
            private int last = -1;

            @Override
            public boolean hasNext() {
                return next >= 0;
            }

            @Override
            public Table next() {
                if (next < 0) {
                    throw new NoSuchElementException();
                }
                Table table = registry.get(next);
                last = next;
                next = bits.nextSetBit(next + 1);
                return table;
            }

            @Override
            public void remove() {
                if (last < 0) {
                    throw new IllegalStateException();
                }
                bits.clear(last);
                last = -1;
            }
        };
    }

    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (Table table : this) {
            result.add(table.toString());
        }
        return result.toString();
    }
}
