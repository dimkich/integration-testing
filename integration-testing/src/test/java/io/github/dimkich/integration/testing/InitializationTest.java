package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.execution.ConstructorMockAnswer;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.RequiredArgsConstructor;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

@MockJavaTime
@SpringBootTest(classes = {IntegrationTestConfig.class, InitializationTest.Config.class})
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
    Stream<DynamicNode> tests() throws Exception {
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
        TestSQLDataStorage testSQLDataStorage() {
            return new TestSQLDataStorage();
        }

        @Bean
        SQLDataStorageService sqlDataStorageService(@Lazy InitializationService initializationService) {
            return new SQLDataStorageService(testSQLDataStorage(), initializationService);
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

    public static class TestSQLDataStorage implements SQLDataStorage {
        private final List<String> operations = new ArrayList<>();

        @Override
        public String getName() {
            return "dataSource";
        }

        @Override
        public void executeSql(Collection<String> sql) throws Exception {
            operations.addAll(sql);
        }

        public List<String> operations() {
            List<String> result = new ArrayList<>(operations);
            operations.clear();
            return result;
        }

        public void reloadT1() {
            operations.add("reloaded t1");
        }

        @Override
        public Map<String, Object> getTablesData(Collection<String> tables, Map<String, Set<String>> excludedRows) throws Exception {
            return Map.of();
        }

        @Override
        public void loadDataset(IDataSet dataSet) throws Exception {
            for (String table : dataSet.getTableNames()) {
                operations.add("Load " + table);
            }
        }

        @Override
        public DatabaseConfig getDbunitConfig() throws Exception {
            return null;
        }

        @Override
        public Set<String> getTables() {
            return Set.of("t1", "t2", "t3");
        }

        @Override
        public void initTablesRestriction(Collection<String> tables) {
        }

        @Override
        public String getAllowTableSql(String table) {
            return "allow " + table;
        }

        @Override
        public String getRestrictTableSql(String table) {
            return "restrict " + table;
        }

        @Override
        public String getClearSql(Collection<String> tables) {
            return "clear " + tables;
        }

        @Override
        public String getRestartIdentitySql(Collection<String> tables) {
            return "restart identity " + tables;
        }
    }
}
