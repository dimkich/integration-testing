package io.github.dimkich.integration.testing.redis.redisson;

import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.client.codec.Codec;

@RequiredArgsConstructor
class RedissonObjectAnswer implements Answer<Object> {
    private final Object targetObject;
    private final String name;
    private final Codec codec;
    private final RedissonMock redissonMock;

    @Override
    public Object answer(InvocationOnMock invocation) {
        return redissonMock.call(targetObject, name, codec, invocation);
    }
}
