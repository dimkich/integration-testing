package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.redis.redisson.RedissonConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@Import(RedissonConfig.class)
@TestPropertySource("classpath:${integration.testing.environment:real}-redis.properties")
public @interface EnableTestRedis {
}