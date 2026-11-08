package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.client.ScoredMember;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only entry point for the framework's own Redis client and pipeline. Used by the
 * {@code framework client} XML container; not part of the production API.
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class RedisClientTestFacade {
    private static final byte[] SET = bytes("SET");
    private static final byte[] GET = bytes("GET");
    private static final byte[] DEL = bytes("DEL");
    private static final byte[] PING = bytes("PING");

    private final Map<String, RedisClient> clients;
    private final RedisProperties properties;

    private RedisClient client(String connection) {
        RedisClient client = clients.get(connection);
        if (client == null) {
            throw new IllegalArgumentException("Unknown Redis client: " + connection);
        }
        return client;
    }

    public String name(String connection) {
        return client(connection).getName();
    }

    public String connectionError(String name) {
        try {
            properties.getConnection(name);
            return "no error";
        } catch (IllegalStateException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public int defaultDatabase(String connection) {
        return client(connection).getDefaultDatabase();
    }

    public String execute(String connection, String command, String... args) {
        byte[][] rawArgs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            rawArgs[i] = bytes(args[i]);
        }
        return text(client(connection).execute(bytes(command), rawArgs));
    }

    public void configSet(String connection, String parameter, String value) {
        client(connection).configSet(parameter, value);
    }

    public String configGet(String connection, String parameter) {
        return execute(connection, "CONFIG", "GET", parameter);
    }

    public void publish(String connection, String channel, String message) {
        client(connection).publish(bytes(channel), bytes(message));
    }

    public void select(String connection, int database) {
        client(connection).select(database);
    }

    public void close(String connection) {
        client(connection).close();
    }

    public void flushAll(String connection) {
        client(connection).flushAll();
    }

    /**
     * Runs {@code FLUSHDB} on the framework connection, in the default database. Issued through
     * the same connection as the sync barrier, so replication order after this call is exact.
     */
    public void flushDb(String connection) {
        RedisClient client = client(connection);
        client.select(client.getDefaultDatabase());
        client.execute(bytes("FLUSHDB"));
    }

    public String selectAndReconnect(String connection, int database, String key, String value) {
        RedisClient client = client(connection);
        client.select(database);
        client.execute(SET, bytes(key), bytes(value));
        client.close();
        String result = text(client.execute(GET, bytes(key)));
        client.execute(DEL, bytes(key));
        client.select(0);
        return result;
    }

    public String replyTypes(String connection) {
        RedisClient client = client(connection);
        byte[] key = bytes("it:client:reply");
        client.execute(SET, key, bytes("1"));
        Object incr = client.execute(bytes("INCR"), key);
        Object missing = client.execute(GET, bytes("it:client:missing"));
        Object keys = client.execute(bytes("KEYS"), key);
        String ping = text(client.execute(PING));
        client.execute(DEL, key);
        return "ping=" + ping + " missing=" + missing + " incr=" + incr
                + " keys=" + ((Object[]) keys).length;
    }

    public String wrongTypeError(String connection) {
        RedisClient client = client(connection);
        byte[] key = bytes("it:client:wrong");
        client.execute(SET, key, bytes("text"));
        try {
            client.execute(bytes("INCR"), key);
            return "no error";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            client.execute(DEL, key);
        }
    }

    public String closeTwice(String connection) {
        RedisClient client = client(connection);
        client.close();
        client.close();
        return text(client.execute(PING));
    }

    public String pipelineTypedCommands(String connection) {
        RedisClient client = client(connection);
        byte[] hash = bytes("it:pipeline:hash");
        byte[] list = bytes("it:pipeline:list");
        byte[] set = bytes("it:pipeline:set");
        byte[] zset = bytes("it:pipeline:zset");
        byte[] stream = bytes("it:pipeline:stream");
        byte[] hll = bytes("it:pipeline:hll");
        byte[] generic = bytes("it:pipeline:generic");
        byte[] eval = bytes("it:pipeline:eval");

        Map<byte[], byte[]> fields = new LinkedHashMap<>();
        fields.put(bytes("f1"), bytes("v1"));
        fields.put(bytes("f2"), bytes("v2"));

        Map<byte[], byte[]> streamFields = new LinkedHashMap<>();
        streamFields.put(bytes("sensor"), bytes("S1"));

        try (RedisPipeline pipeline = client.pipeline()) {
            pipeline.del();
            pipeline.del(hash, list, set, zset, stream, hll, generic, eval);
            pipeline.hSet(hash, fields);
            pipeline.hDel(hash, bytes("missing"));
            pipeline.rPush(list, bytes("a"), bytes("b"));
            pipeline.sAdd(set, bytes("a"), bytes("b"), bytes("c"));
            pipeline.zAdd(zset, List.of(
                    new ScoredMember(bytes("a"), 1.0),
                    new ScoredMember(bytes("b"), 2.0)));
            pipeline.xAdd(stream, bytes("1-0"), streamFields);
            pipeline.pfAdd(hll, bytes("a"), bytes("b"), bytes("c"));
            pipeline.pExpireAt(list, System.currentTimeMillis() + 60_000);
            pipeline.execute(SET, generic, bytes("g"));
            pipeline.eval(bytes("return redis.call('SET', KEYS[1], ARGV[1])"), 1, eval, bytes("e"));
        }

        String result = "h=" + execute(connection, "HLEN", "it:pipeline:hash")
                + " l=" + execute(connection, "LLEN", "it:pipeline:list")
                + " s=" + execute(connection, "SCARD", "it:pipeline:set")
                + " z=" + execute(connection, "ZCARD", "it:pipeline:zset")
                + " x=" + execute(connection, "XLEN", "it:pipeline:stream")
                + " hll>0=" + (parseLong(execute(connection, "PFCOUNT", "it:pipeline:hll")) > 0)
                + " generic=" + execute(connection, "GET", "it:pipeline:generic")
                + " eval=" + execute(connection, "GET", "it:pipeline:eval");
        client.execute(DEL, hash, list, set, zset, stream, hll, generic, eval);
        return result;
    }

    public String pipelineBulkHash(String connection, int count) {
        byte[] key = bytes("it:pipeline:bulk:hash");
        Map<byte[], byte[]> fields = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            fields.put(bytes("f" + i), bytes("v" + i));
        }
        try (RedisPipeline pipeline = client(connection).pipeline()) {
            pipeline.del(key);
            pipeline.hSet(key, fields);
        }
        String result = execute(connection, "HLEN", "it:pipeline:bulk:hash");
        client(connection).execute(DEL, key);
        return result;
    }

    public String pipelineBulkCollections(String connection, int count) {
        byte[] list = bytes("it:pipeline:bulk:list");
        byte[] set = bytes("it:pipeline:bulk:set");
        byte[] zset = bytes("it:pipeline:bulk:zset");
        byte[] hll = bytes("it:pipeline:bulk:hll");

        byte[][] values = new byte[count][];
        byte[][] members = new byte[count][];
        byte[][] elements = new byte[count][];
        List<ScoredMember> scored = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values[i] = bytes("v" + i);
            members[i] = bytes("m" + i);
            elements[i] = bytes("e" + i);
            scored.add(new ScoredMember(members[i], i));
        }

        try (RedisPipeline pipeline = client(connection).pipeline()) {
            pipeline.del(list, set, zset, hll);
            pipeline.rPush(list, values);
            pipeline.sAdd(set, members);
            pipeline.zAdd(zset, scored);
            pipeline.pfAdd(hll, elements);
        }

        String result = "l=" + execute(connection, "LLEN", "it:pipeline:bulk:list")
                + " s=" + execute(connection, "SCARD", "it:pipeline:bulk:set")
                + " z=" + execute(connection, "ZCARD", "it:pipeline:bulk:zset")
                + " hll>0=" + (parseLong(execute(connection, "PFCOUNT", "it:pipeline:bulk:hll")) > 0);
        client(connection).execute(DEL, list, set, zset, hll);
        return result;
    }

    public String pipelineEvalAndExecute(String connection) {
        byte[] eval = bytes("it:pipeline:eval:command");
        byte[] direct = bytes("it:pipeline:execute:command");
        try (RedisPipeline pipeline = client(connection).pipeline()) {
            pipeline.del(eval, direct);
            pipeline.eval(bytes("return redis.call('SET', KEYS[1], ARGV[1])"), 1, eval, bytes("eval"));
            pipeline.execute(SET, direct, bytes("direct"));
        }
        String result = "eval=" + execute(connection, "GET", "it:pipeline:eval:command")
                + " direct=" + execute(connection, "GET", "it:pipeline:execute:command");
        client(connection).execute(DEL, eval, direct);
        return result;
    }

    /**
     * Queues {@code INCR} against a string key. The error is detected when the pipeline is
     * closed; the expected {@code RedisClientException} is returned to the XML assertion.
     */
    public String pipelineError(String connection) {
        RedisClient client = client(connection);
        byte[] key = bytes("it:pipeline:error");
        client.execute(SET, key, bytes("not-a-number"));
        try (RedisPipeline pipeline = client.pipeline()) {
            pipeline.execute(bytes("INCR"), key);
            return "no error";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            client.execute(DEL, key);
        }
    }

    public String pipelineReusable(String connection) {
        return execute(connection, "PING");
    }

    public String pipelineHashTtlAggregation(String connection) {
        byte[] key = bytes("it:pipeline:hash:ttl");
        long now = System.currentTimeMillis();
        Map<byte[], byte[]> fields = new LinkedHashMap<>();
        fields.put(bytes("f1"), bytes("v1"));
        fields.put(bytes("f2"), bytes("v2"));
        fields.put(bytes("f3"), bytes("v3"));

        try (RedisPipeline pipeline = client(connection).pipeline()) {
            pipeline.del(key);
            pipeline.hSet(key, fields);
            pipeline.hExpireAt(key, bytes("f1"), now + 10_000);
            pipeline.hExpireAt(key, bytes("f2"), now + 20_000);
            pipeline.hExpireAt(key, bytes("f3"), now + 30_000);
        }

        Object[] ttls = (Object[]) client(connection).execute(bytes("HPTTL"), bytes("it:pipeline:hash:ttl"),
                bytes("FIELDS"), bytes("3"), bytes("f1"), bytes("f2"), bytes("f3"));
        long first = ((Number) ttls[0]).longValue();
        long second = ((Number) ttls[1]).longValue();
        long third = ((Number) ttls[2]).longValue();
        String result = "positive=" + (first > 0 && second > 0 && third > 0)
                + " ordered=" + (first < second && second < third);
        client(connection).execute(DEL, key);
        return result;
    }

    /**
     * Writes a hash with an expiring field into two logical databases of the same pipeline.
     * Regression check: field TTLs queued for an earlier database must not be executed after a
     * later {@code SELECT}.
     */
    public String pipelineHashTtlAcrossDatabases(String connection) {
        RedisClient client = client(connection);
        byte[] key = bytes("it:pipeline:hash:ttl:db");
        long now = System.currentTimeMillis();
        Map<byte[], byte[]> fields = Map.of(bytes("f"), bytes("v"));

        try (RedisPipeline pipeline = client.pipeline()) {
            pipeline.select(0);
            pipeline.del(key);
            pipeline.hSet(key, fields);
            pipeline.hExpireAt(key, bytes("f"), now + 10_000);
            pipeline.select(1);
            pipeline.del(key);
            pipeline.hSet(key, fields);
            pipeline.hExpireAt(key, bytes("f"), now + 10_000);
        }

        client.select(0);
        long db0 = hashFieldTtl(client, key);
        client.select(1);
        long db1 = hashFieldTtl(client, key);
        client.execute(DEL, key);
        client.select(0);
        client.execute(DEL, key);
        return "db0=" + (db0 > 0) + " db1=" + (db1 > 0);
    }

    public String nestedPipeline(String connection) {
        RedisClient client = client(connection);
        @Cleanup RedisPipeline outer = client.pipeline();
        try {
            @Cleanup RedisPipeline nested = client.pipeline();
            return "no error";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    public String commandDuringPipeline(String connection) {
        RedisClient client = client(connection);
        @Cleanup RedisPipeline pipeline = client.pipeline();
        try {
            client.execute(PING);
            return "no error";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    public String closeDuringPipeline(String connection) {
        RedisClient client = client(connection);
        @Cleanup RedisPipeline pipeline = client.pipeline();
        try {
            client.close();
            return "no error";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    private static long hashFieldTtl(RedisClient client, byte[] key) {
        Object[] ttls = (Object[]) client.execute(bytes("HPTTL"), key,
                bytes("FIELDS"), bytes("1"), bytes("f"));
        return ((Number) ttls[0]).longValue();
    }

    public String pipelineCloseBehavior(String connection) {
        RedisPipeline pipeline = client(connection).pipeline();
        pipeline.close();
        pipeline.flush();
        pipeline.close();
        try {
            pipeline.set(bytes("it:pipeline:closed"), bytes("x"));
            return "no error";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    public String flushAllDatabases(String connection) {
        RedisClient client = client(connection);
        byte[] key = bytes("it:pipeline:flush");
        client.execute(SET, key, bytes("db0"));
        client.select(1);
        client.execute(SET, key, bytes("db1"));
        client.select(0);
        client.flushAll();
        String db0 = text(client.execute(GET, key));
        client.select(1);
        String db1 = text(client.execute(GET, key));
        client.select(0);
        return "db0=" + db0 + " db1=" + db1;
    }

    private static long parseLong(String value) {
        return value == null ? 0 : Long.parseLong(value);
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof Object[] array) {
            String[] parts = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                parts[i] = text(array[i]);
            }
            return "[" + String.join(", ", parts) + "]";
        }
        if (value instanceof List<?> list) {
            String[] parts = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                parts[i] = text(list.get(i));
            }
            return "[" + String.join(", ", parts) + "]";
        }
        return String.valueOf(value);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
