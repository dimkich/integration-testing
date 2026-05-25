package io.github.dimkich.integration.testing.redis;

import com.moilioncircle.redis.replicator.cmd.impl.*;
import com.moilioncircle.redis.replicator.event.*;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueModule;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueModule;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@MockJavaTime(dockerImages = "redis.*")
@EnableTestRedis
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringBootTest(classes = RedisTestConfig.class)
public class RedisTest {
    private final DynamicTestBuilder dynamicTestBuilder;
    private final List<RedisStreamHandler<?>> streamHandlers;
    private final List<RedisSnapshotHandler<?>> snapshotHandlers;
    private static final Set<Class<? extends Event>> notImplementedClasses = Set.of(KeyStringValueModule.class,
            BatchedKeyStringValueModule.class, DumpKeyValuePair.class, PreRdbSyncEvent.class, PostRdbSyncEvent.class,
            PreCommandSyncEvent.class, PostCommandSyncEvent.class, DefaultCommand.class, EvalCommand.class,
            EvalShaCommand.class, ScriptLoadCommand.class, ScriptFlushCommand.class, BRPopLPushCommand.class,
            GeoAddCommand.class
    );

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("redis.xml"
//                ,List.of("redis operations", "redis expiration")
        );
    }

    @org.junit.jupiter.api.Test
    void shouldHandleAllEvents() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Event.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.moilioncircle.redis.replicator");
        List<Class<? extends Event>> unhandledEvents = candidates.stream()
                .map(SneakyFunction.<BeanDefinition, Class<? extends Event>, ClassNotFoundException>sneaky(
                        beanDefinition -> Class.forName(beanDefinition.getBeanClassName()).asSubclass(Event.class)
                ))
                .filter(c -> !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
                .filter(c -> !notImplementedClasses.contains(c))
                .filter(eventClass -> snapshotHandlers.stream().noneMatch(h -> h.canHandle(eventClass)))
                .filter(eventClass -> streamHandlers.stream().noneMatch(h -> h.canHandle(eventClass)))
                .collect(Collectors.toList());
        assertThat(unhandledEvents)
                .as("All events should have a Handler")
                .isEmpty();
    }
}
