package io.github.dimkich.integration.testing.redis.redisson;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.client.codec.Codec;

@Data
@NoArgsConstructor
@AllArgsConstructor
class RedissonObjectAnswer implements Answer<Object> {
    private Object targetObject;
    private String name;
    private Codec codec;
    private RedissonMock redissonMock;
    private Object config;

    @Override
    public Object answer(InvocationOnMock invocation) {
        return redissonMock.call(targetObject, name, codec, invocation, config);
    }
}
