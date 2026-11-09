package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.StubAnswer;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Applies {@link OnTableChangeInit}: for every table in scope whose database revision is ahead of
 * the acknowledged cache revision it invokes the configured bean method and acknowledges the
 * synchronization.
 *
 * <p>Runs after {@link SqlStorageInitSetup} (order 2000) and after {@link
 * io.github.dimkich.integration.testing.initialization.bean.BeanInitSetup} (order 10000), so the
 * actions see the data of all storages already applied. State is not persisted: the action is
 * executed every time it is in scope, which is what keeps a stale cache from a previous test
 * getting caught up.
 */
@Slf4j
@RequiredArgsConstructor
public class OnTableChangeInitSetup implements InitSetup<OnTableChangeInit, OnTableChangeInitState> {
    private final TestDataStorages testDataStorages;
    private final BeanFactory beanFactory;

    @Override
    public Class<OnTableChangeInit> getTestCaseInitClass() {
        return OnTableChangeInit.class;
    }

    @Override
    public OnTableChangeInitState defaultState() {
        return new OnTableChangeInitState();
    }

    @Override
    public OnTableChangeInitState convert(OnTableChangeInit init) {
        SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                SQLDataStorageService.class);
        OnTableChangeInitState state = new OnTableChangeInitState();
        state.put(storage, init.getOnTableChange() == null ? List.of() : init.getOnTableChange());
        return state;
    }

    @Override
    public void apply(OnTableChangeInitState oldState, OnTableChangeInitState newState, Test test) throws Exception {
        StubAnswer.enable(() -> {
            for (Map.Entry<SQLDataStorageService, List<OnTableChangeInit.TableChangeAction>> entry
                    : newState.getActionsByStorage().entrySet()) {
                SQLDataStorageService storage = entry.getKey();
                for (OnTableChangeInit.TableChangeAction action : entry.getValue()) {
                    Table table = storage.resolveTable(action.getTableName());
                    if (!storage.isTableStale(table)) {
                        continue;
                    }
                    log.debug("OnTableChange for storage '{}', table '{}'",
                            storage.getName(), table);
                    invokeAction(action);
                    storage.acknowledgeTableSync(table);
                }
            }
        });
    }

    private void invokeAction(OnTableChangeInit.TableChangeAction action) throws NoSuchMethodException {
        Object bean = beanFactory.getBean(action.getBeanName());
        Method method = ReflectionUtils.findMethod(bean.getClass(), action.getBeanMethod());
        if (method != null) {
            // Invoke on the proxy first so Spring AOP advice (@Transactional, @CacheEvict, security)
            // is not bypassed.
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method, bean);
            return;
        }

        Object target = AopTestUtils.getUltimateTargetObject(bean);
        method = ReflectionUtils.findMethod(target.getClass(), action.getBeanMethod());
        if (method == null) {
            throw new NoSuchMethodException(String.format(
                    "Method '%s()' not found on bean '%s' nor on its target class (%s)",
                    action.getBeanMethod(), action.getBeanName(), target.getClass().getName()));
        }
        ReflectionUtils.makeAccessible(method);
        ReflectionUtils.invokeMethod(method, target);
    }

    @Override
    public boolean saveState() {
        return false;
    }

    @Override
    public Integer getOrder() {
        return 11000;
    }
}
