package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.BeforeTest;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.execution.StubAnswer;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInitState;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import io.github.dimkich.integration.testing.storage.sql.state.StorageState;
import io.github.dimkich.integration.testing.storage.sql.state.TransitionAnalyzer;
import io.github.dimkich.integration.testing.storage.sql.state.TransitionPlan;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.DataSetException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class SQLDataStorageService implements TestDataStorage, BeforeTest {
    private final SQLDataStorage storage;
    private final InitializationService initializationService;

    /**
     * State that became current after the last applyChanges.
     */
    private StorageState currentStorageState;

    @Getter
    private final TableRegistry tableRegistry;
    private final DbUnitFixture fixture;
    private final StorageStateFactory storageStateFactory;
    private final StorageTransitionExecutor transitionExecutor;
    private final TableRevisionTracker revisions;
    private final SequenceResetScheduler sequenceResetScheduler;
    private final TransitionAnalyzer transitionAnalyzer;

    @Override
    @ToString.Include(name = "name")
    public String getName() {
        return storage.getName();
    }

    /**
     * Fully rebuilds the storage after setup DDL or fixture changes: drops cached schema and
     * audit state, reloads tables and metadata, then clears the current state and revisions.
     */
    public void init() throws Exception {
        storage.initialize();
        currentStorageState = null;
        revisions.reset();
        sequenceResetScheduler.reset();
    }

    public Table resolveTable(String name) {
        return tableRegistry.resolve(name);
    }

    public StorageState createDefaultState() {
        return storageStateFactory.createDefault();
    }

    public StorageState createFromInitState(SqlStorageInit init) {
        return storageStateFactory.createFromInit(init);
    }

    public void executeSqls(Collection<String> sqls) throws Exception {
        if (sqls != null && !sqls.isEmpty()) {
            storage.executeSql(sqls, List.of());
        }
    }

    public void setDbUnitXml(Collection<String> paths) throws DataSetException, IOException {
        fixture.setDbUnitXml(paths);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean applyChanges(StorageState oldState, StorageState newState) throws Exception {
        StorageState sourceState = oldState != null ? oldState : new StorageState(tableRegistry);

        transitionAnalyzer.reset(fixture);
        TransitionPlan plan = sourceState.planTransition(newState, transitionAnalyzer);
        log.debug("Init '{}' planned transition: {}", storage.getName(), plan);

        if (!plan.hasChanges()) {
            plan.applyMutationLog(newState);
            currentStorageState = newState;
            return false;
        }

        StubAnswer.enable(() -> transitionExecutor.execute(plan));
        revisions.increment(plan.getChangedTables());
        plan.applyMutationLog(newState);
        if (!plan.getForwardSql().isEmpty()) {
            StorageChanges changes = storage.getStorageChanges(null);
            applyMutations(newState, changes.getMutations());
        }
        currentStorageState = newState;
        return true;
    }

    /**
     * Returns whether the table is stale: its database revision is newer than the revision
     * acknowledged by the application.
     */
    public boolean isTableStale(Table table) {
        return revisions.isStale(table);
    }

    /**
     * Acknowledges that the application has synchronized the table cache.
     */
    public void acknowledgeTableSync(Table table) {
        revisions.acknowledge(table);
    }

    /**
     * Applies mutations to the state and advances the database revisions for the changes made by
     * the framework. SUT mutations are only recorded in the mutation log: the SUT tracks its own
     * changes, so they must neither invalidate the cache nor acknowledge an earlier framework
     * change.
     */
    private void applyMutations(StorageState state, List<StorageMutation> mutations) {
        state.applyMutations(mutations);

        TableSet changedTables = TableSet.empty(tableRegistry);
        for (StorageMutation mutation : mutations) {
            if (mutation.isSutMutation()) {
                continue;
            }
            changedTables.addAll(mutation.getTouchedTables());
        }
        if (!changedTables.isEmpty()) {
            revisions.increment(changedTables);
        }
    }

    /**
     * Empties the given tables on behalf of the application. Pending audit changes are consumed
     * first, so the clear can be planned against an up-to-date state. The clear itself is a normal
     * transition to a target state with explicitly cleared baselines: the transition executes the
     * clear, updates revisions and records a non-revertible history mutation. The audit marker is
     * advanced by the read, so records of the cleared changes are not reported by the next diff.
     */
    public void clearTables(Collection<String> tableNames) throws Exception {
        TableSet tables = TableSet.empty(tableRegistry);
        for (String table : tableNames) {
            tables.add(tableRegistry.resolveOrRegister(TableName.parse(table)));
        }

        StorageState source = currentStorageState != null
                ? currentStorageState
                : new StorageState(tableRegistry);

        // Consume the pending audit before planning: otherwise the target state would be built
        // from stale history. The read advances the audit marker, and applying the mutations
        // keeps the cleared changes out of the next diff. These mutations are SUT changes and are
        // deliberately not passed to the revision tracker: the SUT tracks them itself.
        StorageChanges changes = storage.getStorageChanges(null);
        source.applyMutations(changes.getMutations());

        StorageState target = source.copy();
        for (Table table : tables) {
            target.markTableCleared(table);
        }
        applyChanges(source, target);

        // The source state is the state stored in the init stack. getDiff() applies audit
        // mutations to a copy of that state, so it must carry the same cleared baselines;
        // otherwise the next init would assume the old loaded state is still current.
        for (Table table : tables) {
            source.markTableCleared(table);
        }
    }

    /**
     * {@link BeforeTest} hook: ensures that the first executable test of a scenario starts with
     * reset sequences. Later scenarios receive the reset as part of the previous audit read, so
     * no separate database round trip is needed before every test.
     */
    @Override
    public void before(Test test) throws Exception {
        sequenceResetScheduler.before(test);
    }

    @Override
    public Object getDiff() throws Exception {
        String resetSql = sequenceResetScheduler.pendingResetSql();
        StorageChanges result = storage.getStorageChanges(resetSql);
        sequenceResetScheduler.clearPending();
        SqlStorageInitState state = initializationService.changeCurrentStatus(SqlStorageInit.class);
        if (state != null) {
            StorageState storageState = state.getStorageState(this);
            if (storageState != null) {
                applyMutations(storageState, result.getMutations());
                // changeCurrentStatus may return a copy; it is the state used by the next
                // transition, so the current state must follow it.
                currentStorageState = storageState;
            }
        }
        return result.getDiff();
    }
}
