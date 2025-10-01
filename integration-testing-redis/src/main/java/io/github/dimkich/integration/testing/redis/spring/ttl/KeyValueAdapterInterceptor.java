package io.github.dimkich.integration.testing.redis.spring.ttl;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.data.redis.core.PartialUpdate;
import org.springframework.data.redis.core.mapping.RedisPersistentEntity;
import org.springframework.data.util.CloseableIterator;

import java.lang.reflect.Parameter;

@RequiredArgsConstructor
public class KeyValueAdapterInterceptor implements MethodInterceptor {
    private final TtlEntityService ttlEntityService;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        if (invocation.getMethod().getName().equals("put")) {
            ttlEntityService.saveTtl(args[0], args[1], (String) args[2]);
            return invocation.proceed();
        } else if (invocation.getMethod().getName().equals("update")) {
            PartialUpdate<?> update = (PartialUpdate<?>) args[0];
            RedisPersistentEntity<?> entity = ttlEntityService.getConverter().getMappingContext()
                    .getRequiredPersistentEntity(update.getTarget());
            String keyspace = entity.getKeySpace();
            ttlEntityService.saveTtl(update.getId(), update.getValue(), keyspace);
            return invocation.proceed();
        } else if (invocation.getMethod().getName().equals("deleteAllOf")) {
            ttlEntityService.clear((String) args[0]);
            return invocation.proceed();
        }
        String keyspace = null;
        Parameter[] parameters = invocation.getMethod().getParameters();
        for (int i = 0; i < args.length; i++) {
            if ("keyspace".equals(parameters[i].getName())) {
                keyspace = (String) args[i];
                break;
            }
        }
        Object result = invocation.proceed();

        if (result == null) {
            return null;
        } else if (result instanceof Iterable<?> iterable) {
            for (Object entity : iterable) {
                ttlEntityService.restoreTtl(entity, keyspace);
            }
        } else if (result instanceof CloseableIterator<?>) {
            throw new UnsupportedOperationException("Not yet implemented");
        } else {
            ttlEntityService.restoreTtl(result, keyspace);
        }
        return result;
    }
}
