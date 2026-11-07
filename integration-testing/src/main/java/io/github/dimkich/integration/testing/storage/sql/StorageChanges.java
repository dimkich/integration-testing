package io.github.dimkich.integration.testing.storage.sql;

import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Lazy result of one storage read. Each accessor computes its value on first use and reuses it
 * afterward; the two accessors stay independent and do not mutate the storage.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class StorageChanges {
    private final SneakySupplier<Object, Exception> diffSupplier;
    private final SneakySupplier<List<StorageMutation>, Exception> mutationsSupplier;

    private Object diff;
    private boolean diffResolved;
    private List<StorageMutation> mutations;
    private boolean mutationsResolved;

    /**
     * Creates a lazy result. Each accessor calls its supplier on first use and memoizes the
     * resulting value.
     */
    public static StorageChanges of(SneakySupplier<Object, Exception> diff,
                                    SneakySupplier<List<StorageMutation>, Exception> mutations) {
        return new StorageChanges(diff, mutations);
    }

    /**
     * Difference for the SUT changes captured by this read, or null when there were no SUT
     * mutations.
     */
    public Object getDiff() throws Exception {
        if (!diffResolved) {
            diff = diffSupplier.get();
            diffResolved = true;
        }
        return diff;
    }

    /**
     * SQL and SUT mutations with their revert data, in execution order.
     */
    public List<StorageMutation> getMutations() throws Exception {
        if (!mutationsResolved) {
            mutations = mutationsSupplier.get();
            mutationsResolved = true;
        }
        return mutations;
    }
}
