package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.execution.ConstructorMockAnswer;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.SequenceResetScheduler;
import io.github.dimkich.integration.testing.storage.sql.StorageChanges;
import io.github.dimkich.integration.testing.storage.sql.StorageStateFactory;
import io.github.dimkich.integration.testing.storage.sql.StorageTransitionExecutor;
import io.github.dimkich.integration.testing.storage.sql.TableRevisionTracker;
import io.github.dimkich.integration.testing.storage.sql.state.ExactMutationImpact;
import io.github.dimkich.integration.testing.storage.sql.state.MutationImpact;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import io.github.dimkich.integration.testing.storage.sql.state.TransitionAnalyzer;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

@MockJavaTime
@SpringBootTest(classes = InitializationTest.Config.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class InitializationTest {
    private final DynamicTestBuilder dynamicTestBuilder;

    private static MockedConstruction<MockInvoke> mockInvoke;

    @BeforeAll
    static void setUp() {
        mockInvoke = Mockito.mockConstruction(MockInvoke.class,
                Mockito.withSettings().defaultAnswer(new ConstructorMockAnswer(i -> {
                    if (i.getMethod().getName().equals("reset")) {
                        MockInvoke mockInvoke = (MockInvoke) i.getMock();
                        MockResetCounter.count.compute(mockInvoke.getName(), (n, c) -> c == null ? 1 : c + 1);
                    }
                    return i.callRealMethod();
                })).stubOnly(),
                (mock, context) -> {
                    ConstructorMockAnswer ans = (ConstructorMockAnswer) MockUtil.getMockSettings(mock)
                            .getDefaultAnswer();
                    ans.getMockToObject().put(mock, new MockInvoke());
                });
    }

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("initialization.xml");
    }

    @AfterAll
    static void tearDown() {
        mockInvoke.close();
    }

    @Configuration
    static class Config {
        @Bean
        DateTime dateTime() {
            return new DateTime();
        }

        @Bean
        Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        Bean2 bean2() {
            return new Bean2();
        }

        @Bean
        BeanInitCount beanInitCount() {
            return new BeanInitCount(bean1(), bean2());
        }

        @Bean
        MockResetCounter mockResetCounter() {
            return new MockResetCounter();
        }

        @Bean
        TableRegistry tableRegistry() {
            return new TableRegistry("public");
        }

        @Bean
        TestSQLDataStorage testSQLDataStorage(TableRegistry tableRegistry) {
            return new TestSQLDataStorage("dataSource", tableRegistry);
        }

        @Bean
        SequenceTestSQLDataStorage secondarySQLDataStorage(TableRegistry tableRegistry) {
            return new SequenceTestSQLDataStorage("secondaryDataSource", tableRegistry);
        }

        @Bean
        SQLDataStorageService sqlDataStorageService(@Lazy InitializationService initializationService,
                                                    TableRegistry tableRegistry,
                                                    @Qualifier("testSQLDataStorage")
                                                    TestSQLDataStorage testSQLDataStorage) {
            return createStorageService(initializationService, tableRegistry, testSQLDataStorage);
        }

        @Bean
        SQLDataStorageService secondarySQLDataStorageService(@Lazy InitializationService initializationService,
                                                             TableRegistry tableRegistry,
                                                             @Qualifier("secondarySQLDataStorage")
                                                             SequenceTestSQLDataStorage secondarySQLDataStorage) {
            return createStorageService(initializationService, tableRegistry, secondarySQLDataStorage);
        }

        @Bean
        ClearTablesFacade clearTablesFacade(@Qualifier("sqlDataStorageService")
                                            SQLDataStorageService sqlDataStorageService,
                                            @Qualifier("testSQLDataStorage")
                                            TestSQLDataStorage testSQLDataStorage) {
            return new ClearTablesFacade(sqlDataStorageService, testSQLDataStorage);
        }

        @Bean
        StorageOperationsFacade storageOperationsFacade(@Qualifier("testSQLDataStorage")
                                                        TestSQLDataStorage testSQLDataStorage,
                                                        @Qualifier("secondarySQLDataStorage")
                                                        SequenceTestSQLDataStorage secondarySQLDataStorage) {
            return new StorageOperationsFacade(testSQLDataStorage, secondarySQLDataStorage);
        }

        @Bean
        ProxyReloadFacade proxyReloadFacade(ReloadTarget reloadTarget,
                                            @Qualifier("testSQLDataStorage")
                                            TestSQLDataStorage testSQLDataStorage) {
            return new ProxyReloadFacade(reloadTarget, testSQLDataStorage);
        }

        @Bean
        ReloadTarget reloadTarget() {
            return new ReloadTarget();
        }

        @Bean
        Marker reloadProxy(ReloadTarget reloadTarget) {
            ProxyFactory factory = new ProxyFactory(reloadTarget);
            factory.addInterface(Marker.class);
            factory.setProxyTargetClass(false);
            return (Marker) factory.getProxy();
        }

        private static SQLDataStorageService createStorageService(
                InitializationService initializationService,
                TableRegistry tableRegistry,
                TestSQLDataStorage storage) {
            DbUnitFixture fixture = new DbUnitFixture(tableRegistry);
            return new SQLDataStorageService(
                    storage,
                    initializationService,
                    tableRegistry,
                    fixture,
                    new StorageStateFactory(tableRegistry, fixture),
                    new StorageTransitionExecutor(storage, fixture),
                    new TableRevisionTracker(),
                    new SequenceResetScheduler(storage),
                    new TransitionAnalyzer(tableRegistry));
        }

        @Bean
        TestConverter converter() {
            return t -> {
                if (!t.isContainer()) {
                    t.setName(t.getResponse().toString());
                }
            };
        }
    }

    public static class DateTime {
        public ZonedDateTime getDateTime() {
            return ZonedDateTime.now();
        }
    }

    @RequiredArgsConstructor
    public static class BeanInitCount {
        private final Bean1 bean1;
        private final Bean2 bean2;

        public Map<String, Integer> count() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("bean1", bean1.count());
            map.put("bean2", bean2.count());
            return map;
        }
    }

    public static class Bean1 {
        private int count;

        public void init() {
            count++;
        }

        public int count() {
            int c = count;
            count = 0;
            return c;
        }
    }

    public static class Bean2 {
        private int count;

        public void init() {
            count++;
        }

        public int count() {
            int c = count;
            count = 0;
            return c;
        }
    }

    public static class MockResetCounter {
        public static final Map<String, Integer> count = new LinkedHashMap<>();

        public Map<String, Integer> count() {
            Map<String, Integer> c = new LinkedHashMap<>(count);
            count.clear();
            return c;
        }
    }

    @RequiredArgsConstructor
    public static class ClearTablesFacade {
        private final SQLDataStorageService storage;
        private final TestSQLDataStorage testStorage;

        public List<String> clearT1() throws Exception {
            storage.clearTables(List.of("t1"));
            return testStorage.operations();
        }
    }

    @RequiredArgsConstructor
    public static class StorageOperationsFacade {
        private final TestSQLDataStorage testStorage;
        private final SequenceTestSQLDataStorage secondaryStorage;

        public List<String> operations() {
            List<String> result = new ArrayList<>();
            result.addAll(testStorage.operations());
            result.addAll(secondaryStorage.operations());
            return result;
        }
    }

    @RequiredArgsConstructor
    public static class ProxyReloadFacade {
        private final ReloadTarget reloadTarget;
        private final TestSQLDataStorage testStorage;

        public List<String> reloadAndOperations() {
            List<String> result = new ArrayList<>();
            result.add(String.valueOf(reloadTarget.count()));
            result.addAll(testStorage.operations());
            return result;
        }
    }

    public interface Marker {
    }

    public static class ReloadTarget {
        private int count;

        public void reload() {
            count++;
        }

        public int count() {
            return count;
        }
    }

    @RequiredArgsConstructor
    public static class TestSQLDataStorage implements SQLDataStorage {
        @Getter
        private final String name;
        private final TableRegistry registry;
        private final List<String> operations = new ArrayList<>();
        private final Map<String, String> pendingTags = new LinkedHashMap<>();
        private final Map<String, Map<TableName, List<String>>> pendingRevertByTag = new LinkedHashMap<>();
        private final Map<TableName, List<String>> pendingSut = new LinkedHashMap<>();
        private long tagSequence;

        @Override
        public StorageChanges getStorageChanges(String postReadSql) {
            List<StorageMutation> mutations = new ArrayList<>();
            for (Map.Entry<TableName, List<String>> entry : pendingSut.entrySet()) {
                StorageMutation mutation = StorageMutation.builder(registry)
                        .impact(registry.resolve(entry.getKey()), MutationImpact.revertible(1,
                                () -> new ExactMutationImpact(1, entry.getValue())))
                        .build();
                mutations.add(mutation);
            }
            mutations.addAll(buildAppliedSqlMutations(registry));
            pendingSut.clear();
            if (postReadSql != null && !postReadSql.isBlank()) {
                executeSql(List.of(postReadSql), List.of());
            }
            return StorageChanges.of(() -> null, () -> mutations);
        }

        private List<StorageMutation> buildAppliedSqlMutations(TableRegistry registry) {
            List<StorageMutation> appliedSqlMutations = new ArrayList<>(pendingTags.size());
            for (Map.Entry<String, String> tagEntry : pendingTags.entrySet()) {
                StorageMutation.StorageMutationBuilder builder = StorageMutation.builder(registry)
                        .script(tagEntry.getValue());
                Map<TableName, List<String>> revertByTable = pendingRevertByTag.get(tagEntry.getKey());
                if (revertByTable != null) {
                    for (Map.Entry<TableName, List<String>> tableEntry : revertByTable.entrySet()) {
                        builder.impact(registry.resolve(tableEntry.getKey()),
                                MutationImpact.revertible(1,
                                        () -> new ExactMutationImpact(1, tableEntry.getValue())));
                    }
                }
                appliedSqlMutations.add(builder.build());
            }
            pendingTags.clear();
            pendingRevertByTag.clear();
            return appliedSqlMutations;
        }

        @Override
        public void executeSql(Collection<String> untrackedSql, Collection<String> trackedSql) {
            operations.addAll(untrackedSql);
            Map<String, String> taggedSql = new LinkedHashMap<>();
            for (String statement : trackedSql) {
                String tag = "n" + tagSequence++;
                taggedSql.put(tag, statement);
                operations.add(statement);
                Set<TableName> tables = new LinkedHashSet<>();
                for (TableName table : getTables()) {
                    if (statement.matches(".*\\b" + table.getUnqualified() + "\\b.*")) {
                        tables.add(table);
                    }
                }
                if (!tables.isEmpty()) {
                    Map<TableName, List<String>> byTable = new LinkedHashMap<>();
                    for (TableName table : tables) {
                        byTable.put(table, List.of("revert " + table.getUnqualified()));
                    }
                    pendingRevertByTag.put(tag, byTable);
                }
            }
            pendingTags.putAll(taggedSql);
        }

        public List<String> operations() {
            List<String> result = new ArrayList<>(operations);
            operations.clear();
            return result;
        }

        public void reloadT1() {
            operations.add("reloaded t1");
        }

        /**
         * Simulates a SUT mutation: data changed by the application outside an init. Returns and clears
         * the accumulated operations so that the expectations of the following tests are not mixed
         * with the current ones.
         */
        public List<String> sutMutationT1() {
            pendingSut.put(TableName.unqualified("t1"), List.of("revert t1"));
            return operations();
        }

        @Override
        public void loadFixture(DbUnitFixture fixture, TableSet tables) {
            for (Table table : tables) {
                operations.add("Load " + table.getUnqualified());
            }
        }

        @Override
        public void initialize() {
            reset();
            registry.updateTables(getTables());
        }

        private Set<TableName> getTables() {
            return new LinkedHashSet<>(List.of(
                    TableName.unqualified("t1"),
                    TableName.unqualified("t2"),
                    TableName.unqualified("t3")));
        }

        @Override
        public String getClearSql(TableSet tables) {
            List<String> names = new ArrayList<>();
            for (Table table : tables) {
                names.add(table.getUnqualified());
            }
            return "clear " + names;
        }

        @Override
        public String getSequenceRestartSql() {
            return "";
        }

        private void reset() {
            pendingTags.clear();
            pendingRevertByTag.clear();
            pendingSut.clear();
        }
    }

    public static class SequenceTestSQLDataStorage extends TestSQLDataStorage {
        private boolean sequenceRestartSqlReturned;

        public SequenceTestSQLDataStorage(String name, TableRegistry registry) {
            super(name, registry);
        }

        @Override
        public void initialize() {
            super.initialize();
            sequenceRestartSqlReturned = false;
        }

        @Override
        public String getSequenceRestartSql() {
            if (sequenceRestartSqlReturned) {
                return "";
            }
            sequenceRestartSqlReturned = true;
            return "restart sequences";
        }
    }
}
