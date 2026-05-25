package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class RedisTestFacade<K, V> {

    private final RedisTemplate<K, V> redisTemplate;
    private final AtomicLong tempKeyCounter = new AtomicLong(0);

    public RedisTestFacade(RedisTemplate<K, V> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- Strings ---
    public void set(K key, V dto) {
        redisTemplate.opsForValue().set(key, dto);
    }

    public V get(K key) {
        return redisTemplate.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public V get(K key, int db) {
        return redisTemplate.execute((RedisConnection connection) -> {
            connection.select(db);
            RedisSerializer<Object> keySerializer = (RedisSerializer<Object>) redisTemplate.getKeySerializer();
            byte[] serializedKey = keySerializer.serialize(key);
            byte[] valueBytes = connection.stringCommands().get(Objects.requireNonNull(serializedKey));
            return (V) redisTemplate.getValueSerializer().deserialize(valueBytes);
        });
    }

    public Boolean setNX(K key, V value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public void mSet(Map<K, V> map) {
        redisTemplate.opsForValue().multiSet(map);
    }

    public Boolean mSetNX(Map<K, V> map) {
        return redisTemplate.opsForValue().multiSetIfAbsent(map);
    }

    public Long incrBy(K key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Double incrByFloat(K key, double delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Integer append(K key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    public Long incr(K key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long decr(K key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decrBy(K key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public V getSet(K key, V value) {
        return redisTemplate.opsForValue().getAndSet(key, value);
    }

    public void setRange(K key, long offset, V value) {
        redisTemplate.opsForValue().set(key, value, offset);
    }

    public void setEx(K key, V value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    public void pSetEx(K key, V value, long milliseconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(milliseconds));
    }

    public Boolean setBit(K key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    public Boolean getBit(K key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    @SuppressWarnings("unchecked")
    public Long bitOp(RedisStringCommands.BitOperation op, K destKey, K... sourceKeys) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            byte[] rawDestKey = ((RedisSerializer<K>) redisTemplate.getKeySerializer()).serialize(destKey);
            byte[][] rawSourceKeys = new byte[sourceKeys.length][];
            for (int i = 0; i < sourceKeys.length; i++) {
                rawSourceKeys[i] = ((RedisSerializer<K>) redisTemplate.getKeySerializer()).serialize(sourceKeys[i]);
            }
            return connection.stringCommands().bitOp(op, Objects.requireNonNull(rawDestKey), rawSourceKeys);
        });
    }

    public List<Long> bitField(K key, BitFieldSubCommands subCommands) {
        return redisTemplate.opsForValue().bitField(key, subCommands);
    }

    @SafeVarargs
    public final Long pfAdd(K key, V... elements) {
        return redisTemplate.opsForHyperLogLog().add(key, elements);
    }

    @SuppressWarnings("unchecked")
    public Long pfCount(K... keys) {
        return redisTemplate.opsForHyperLogLog().size(keys);
    }

    @SuppressWarnings("unchecked")
    public Long pfMerge(K destKey, K... sourceKeys) {
        return redisTemplate.opsForHyperLogLog().union(destKey, sourceKeys);
    }

    // --- Hashes ---
    public <HK> void hSet(K key, HK hashKey, V dto) {
        redisTemplate.opsForHash().put(key, hashKey, dto);
    }

    @SuppressWarnings("unchecked")
    public <HK> V hGet(K key, HK hashKey) {
        return (V) redisTemplate.opsForHash().get(key, hashKey);
    }

    @SuppressWarnings("unchecked")
    public <HK> Map<HK, V> hGetAll(K key) {
        return (Map<HK, V>) redisTemplate.opsForHash().entries(key);
    }

    public <HK> Long hDel(K key, HK hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    public <HK> Boolean hSetNx(K key, HK hashKey, V value) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    public <HK> Long hIncrBy(K key, HK hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    public Double hIncrByFloat(K key, Object hashKey, double delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    public void hMSet(K key, Map<Object, V> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    public void hExpire(K key, long seconds, String flag, String[] fields) {
        String script =
                "local cmd = {'HEXPIRE', KEYS[1], ARGV[1]} " +
                        "if ARGV[2] ~= '' then " +
                        "  table.insert(cmd, ARGV[2]) " +
                        "end " +
                        "table.insert(cmd, 'FIELDS') " +
                        "table.insert(cmd, tostring(#ARGV - 2)) " +
                        "for i = 3, #ARGV do " +
                        "  table.insert(cmd, ARGV[i]) " +
                        "end " +
                        "return redis.call(unpack(cmd))";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        Object[] luaArgs = new Object[fields.length + 2];
        luaArgs[0] = String.valueOf(seconds);
        luaArgs[1] = (flag == null) ? "" : flag.toUpperCase().trim();
        System.arraycopy(fields, 0, luaArgs, 2, fields.length);
        RedisSerializer<String> argsSerializer = redisTemplate.getStringSerializer();
        RedisSerializer<Long> resultSerializer = new GenericToStringSerializer<>(Long.class);

        redisTemplate.execute(
                redisScript,
                argsSerializer,
                resultSerializer,
                Collections.singletonList(key),
                luaArgs
        );
    }

    public void hPersist(K key, String[] fields) {
        String script =
                "local cmd = {'HPERSIST', KEYS[1], 'FIELDS', tostring(#ARGV)} " +
                        "for i = 1, #ARGV do " +
                        "  table.insert(cmd, ARGV[i]) " +
                        "end " +
                        "return redis.call(unpack(cmd))";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        redisTemplate.execute(
                redisScript,
                redisTemplate.getStringSerializer(),
                new GenericToStringSerializer<>(Long.class),
                Collections.singletonList(key),
                (Object[]) fields
        );
    }

    @SuppressWarnings("unchecked")
    public List<Long> hTtl(K key, String[] fields) {
        String script = "return redis.call('HTTL', KEYS[1], 'FIELDS', #ARGV, unpack(ARGV))";
        RedisScript<List<Long>> redisScript = new DefaultRedisScript(script, List.class);
        return redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                (Object[]) fields
        );
    }

    // --- Lists ---
    public List<V> lRange(K key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @SafeVarargs
    public final Long lPush(K key, V... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    @SafeVarargs
    public final Long rPush(K key, V... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    public V lPop(K key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public List<V> lPop(K key, long count) {
        return redisTemplate.opsForList().leftPop(key, count);
    }

    public V rPop(K key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public List<V> rPop(K key, long count) {
        return redisTemplate.opsForList().rightPop(key, count);
    }

    public void lSet(K key, long index, V value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    public Long lRem(K key, long count, V value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    public void lTrim(K key, long start, long stop) {
        redisTemplate.opsForList().trim(key, start, stop);
    }

    public Long lInsertBefore(K key, V pivot, V value) {
        return redisTemplate.opsForList().leftPush(key, pivot, value);
    }

    public Long lInsertAfter(K key, V pivot, V value) {
        return redisTemplate.opsForList().rightPush(key, pivot, value);
    }

    public V lMove(K sourceKey, K destKey, RedisListCommands.Direction from, RedisListCommands.Direction to) {
        return redisTemplate.opsForList().move(sourceKey, from, destKey, to);
    }

    public final Long lPushX(K key, V value) {
        return redisTemplate.opsForList().leftPushIfPresent(key, value);
    }

    public Long rPushX(K key, V value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, value);
    }

    public V rPopLPush(K source, K destination) {
        return redisTemplate.opsForList().rightPopAndLeftPush(source, destination);
    }

    public V brPopLPush(K source, K destination, long timeoutSeconds) {
        return redisTemplate.opsForList().rightPopAndLeftPush(source, destination, Duration.ofSeconds(timeoutSeconds));
    }

    @SuppressWarnings("deprecation")
    public V blMove(K sourceKey, K destKey, RedisListCommands.Direction from, RedisListCommands.Direction to, long timeoutSeconds) {
        String script = "return redis.call('BLMOVE', KEYS[1], KEYS[2], ARGV[1], ARGV[2], ARGV[3])";
        return redisTemplate.execute((RedisCallback<V>) connection -> {
            byte[] rawSource = serializeKey(sourceKey);
            byte[] rawDest = serializeKey(destKey);

            byte[][] args = {
                    from.name().getBytes(StandardCharsets.UTF_8),
                    to.name().getBytes(StandardCharsets.UTF_8),
                    String.valueOf(timeoutSeconds).getBytes(StandardCharsets.UTF_8)
            };
            Object result = connection.eval(
                    script.getBytes(StandardCharsets.UTF_8),
                    ReturnType.VALUE,
                    2,
                    rawSource, rawDest,
                    args[0], args[1], args[2]
            );
            return result != null ? deserializeValue((byte[]) result) : null;
        });
    }

    // --- Sets ---
    @SuppressWarnings("unchecked")
    public Long sAdd(K key, V... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    @SuppressWarnings("unchecked")
    public Long sRem(K key, V... values) {
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }

    public Boolean sMove(K sourceKey, V member, K destinationKey) {
        return redisTemplate.opsForSet().move(sourceKey, member, destinationKey);
    }

    public Long sUnionStore(K key, Collection<K> otherKeys, K destinationKey) {
        return redisTemplate.opsForSet().unionAndStore(key, otherKeys, destinationKey);
    }

    public Long sInterStore(K key, Collection<K> otherKeys, K destinationKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKeys, destinationKey);
    }

    public Long sDiffStore(K key, Collection<K> otherKeys, K destinationKey) {
        return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, destinationKey);
    }

    // --- Sets ---
    public Boolean zAdd(K key, V value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Boolean zAddNX(K key, V value, double score) {
        return zAddInternal(key, value, score, RedisZSetCommands.ZAddArgs.ifNotExists());
    }

    public Boolean zAddXX(K key, V value, double score) {
        return zAddInternal(key, value, score, RedisZSetCommands.ZAddArgs.ifExists());
    }

    public Boolean zAddGT(K key, V value, double score) {
        return zAddInternal(key, value, score, RedisZSetCommands.ZAddArgs.empty().gt());
    }

    public Boolean zAddLT(K key, V value, double score) {
        return zAddInternal(key, value, score, RedisZSetCommands.ZAddArgs.empty().lt());
    }

    @SuppressWarnings("unchecked")
    private Boolean zAddInternal(K key, V value, double score, RedisZSetCommands.ZAddArgs args) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            byte[] rawKey = ((RedisSerializer<K>) redisTemplate.getKeySerializer()).serialize(key);
            byte[] rawValue = ((RedisSerializer<V>) redisTemplate.getValueSerializer()).serialize(value);
            return connection.zSetCommands().zAdd(Objects.requireNonNull(rawKey), score, Objects.requireNonNull(rawValue), args);
        });
    }

    public Double zIncrBy(K key, V value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    @SafeVarargs
    public final Long zRem(K key, V... values) {
        return redisTemplate.opsForZSet().remove(key, (Object[]) values);
    }

    public Long zRemRangeByRank(K key, long start, long stop) {
        return redisTemplate.opsForZSet().removeRange(key, start, stop);
    }

    @SuppressWarnings("unchecked")
    public Long zRemRangeByScore(K key, String minStr, String maxStr) {
        Range.Bound<Double> minBound = parseBound(minStr);
        Range.Bound<Double> maxBound = parseBound(maxStr);

        return redisTemplate.execute((RedisConnection connection) -> {
            byte[] rawKey = ((RedisSerializer<K>) redisTemplate.getKeySerializer()).serialize(key);
            return connection.zSetCommands().zRemRangeByScore(
                    Objects.requireNonNull(rawKey),
                    Range.of(minBound, maxBound)
            );
        }, true);
    }

    private Range.Bound<Double> parseBound(String value) {
        if (value == null || "-inf".equalsIgnoreCase(value) || "+inf".equalsIgnoreCase(value)) {
            return Range.Bound.unbounded();
        }
        if (value.startsWith("(")) {
            return Range.Bound.exclusive(Double.parseDouble(value.substring(1)));
        }
        return Range.Bound.inclusive(Double.parseDouble(value));
    }

    public V zPopMax(K key) {
        try {
            ZSetOperations.TypedTuple<V> tuple = redisTemplate.opsForZSet().popMax(key);
            return tuple != null ? tuple.getValue() : null;
        } catch (InvalidDataAccessApiUsageException e) {
            Throwable throwable = e;
            while (throwable.getCause() != null) {
                throwable = throwable.getCause();
                if (throwable instanceof IndexOutOfBoundsException) {
                    return null;
                }
            }
            throw e;
        }
    }

    public Set<V> zPopMax(K key, long count) {
        return redisTemplate.execute((RedisCallback<Set<V>>) connection -> {
            byte[] rawKey = serializeKey(key);
            Set<Tuple> tuples = connection.zSetCommands().zPopMax(rawKey, count);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptySet();
            }

            return tuples.stream()
                    .map(tuple -> deserializeValue(tuple.getValue()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });
    }

    public V zPopMin(K key) {
        ZSetOperations.TypedTuple<V> tuple = redisTemplate.opsForZSet().popMin(key);
        return tuple != null ? tuple.getValue() : null;
    }

    public Set<V> zPopMin(K key, long count) {
        return redisTemplate.execute((RedisCallback<Set<V>>) connection -> {
            byte[] rawKey = serializeKey(key);
            Set<Tuple> tuples = connection.zSetCommands().zPopMin(rawKey, count);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptySet();
            }

            return tuples.stream()
                    .map(tuple -> deserializeValue(tuple.getValue()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });
    }

    public Long zRemRangeByLex(K key, String min, String max) {
        return redisTemplate.opsForZSet().removeRangeByLex(key, parseLexRange(min, max));
    }

    private Range<String> parseLexRange(String min, String max) {
        return Range.of(
                parseLexBound(min, true),
                parseLexBound(max, false)
        );
    }

    private Range.Bound<String> parseLexBound(String str, boolean isLower) {
        if (isLower && "-".equals(str)) {
            return Range.Bound.unbounded();
        }
        if (!isLower && "+".equals(str)) {
            return Range.Bound.unbounded();
        }

        if (str.startsWith("[")) {
            return Range.Bound.inclusive(str.substring(1));
        }
        if (str.startsWith("(")) {
            return Range.Bound.exclusive(str.substring(1));
        }
        return Range.Bound.inclusive(str);
    }

    public Long zUnionStore(K key, Collection<K> otherKeys, K destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
    }

    public Long zUnionStore(K key, List<K> otherKeys, K destKey, Aggregate aggregate, double[] weights) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey, aggregate, Weights.of(weights));
    }

    public Long zInterStore(K key, List<K> otherKeys, K destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey);
    }

    public Long zInterStore(K key, List<K> otherKeys, K destKey, Aggregate aggregate, double[] weights) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey, aggregate, Weights.of(weights));
    }

    public Long zDiffStore(K key, List<K> otherKeys, K destKey) {
        return redisTemplate.opsForZSet().differenceAndStore(key, otherKeys, destKey);
    }

    public Long zRangeStoreByScore(K srcKey, K destKey, Double min, Double max) {
        return redisTemplate.opsForZSet().rangeAndStoreByScore(srcKey, destKey, Range.closed(min, max));
    }

    public Long zRangeStoreByScore(K srcKey, K destKey, Range<? extends Number> range, long offset, long count) {
        return redisTemplate.opsForZSet().rangeAndStoreByScore(srcKey, destKey, range,
                Limit.limit().offset((int) offset).count((int) count));
    }

    public Long zRangeStore(K srcKey, K destKey, long start, long stop) {
        String script = "return redis.call('ZRANGESTORE', KEYS[1], KEYS[2], ARGV[1], ARGV[2])";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        return redisTemplate.execute(redisScript,
                List.of(destKey, srcKey),
                String.valueOf(start),
                String.valueOf(stop)
        );
    }

    public Long zRangeStoreByLex(K srcKey, K destKey, String min, String max) {
        return redisTemplate.opsForZSet().rangeAndStoreByLex(srcKey, destKey, parseLexRange(min, max));
    }

    public Long zRangeStoreByScoreRev(K srcKey, K destKey, Double min, Double max, long offset, long count) {
        String script = "return redis.call('ZRANGESTORE', KEYS[1], KEYS[2], ARGV[1], ARGV[2], 'BYSCORE', 'REV', 'LIMIT', ARGV[3], ARGV[4])";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        return redisTemplate.execute(redisScript,
                List.of(destKey, srcKey),
                String.valueOf(max),
                String.valueOf(min),
                String.valueOf(offset),
                String.valueOf(count)
        );
    }

    // --- Geo ---

    public Long geoAdd(K key, double lon, double lat, V member) {
        return redisTemplate.opsForGeo().add(key, new org.springframework.data.geo.Point(lon, lat), member);
    }

    public Long geoSearchStore(K destKey, K srcKey, double lon, double lat, double radius) {
        byte[] script = "return redis.call('GEOSEARCHSTORE', KEYS[1], KEYS[2], 'FROMLONLAT', ARGV[1], ARGV[2], 'BYRADIUS', ARGV[3], 'km')".getBytes(StandardCharsets.UTF_8);
        return executeGeoLua(script, Arrays.asList(destKey, srcKey),
                String.valueOf(lon), String.valueOf(lat), String.valueOf(radius));
    }

    public Long geoSearchStoreBox(K destKey, K srcKey, double lon, double lat, double width, double height) {
        byte[] script = "return redis.call('GEOSEARCHSTORE', KEYS[1], KEYS[2], 'FROMLONLAT', ARGV[1], ARGV[2], 'BYBOX', ARGV[3], ARGV[4], 'km')".getBytes(StandardCharsets.UTF_8);
        return executeGeoLua(script, Arrays.asList(destKey, srcKey),
                String.valueOf(lon), String.valueOf(lat), String.valueOf(width), String.valueOf(height));
    }

    public Long geoSearchStoreLimit(K destKey, K srcKey, double lon, double lat, int count) {
        byte[] script = "return redis.call('GEOSEARCHSTORE', KEYS[1], KEYS[2], 'FROMLONLAT', ARGV[1], ARGV[2], 'BYRADIUS', '100', 'km', 'ASC', 'COUNT', ARGV[3])".getBytes(StandardCharsets.UTF_8);
        return executeGeoLua(script, Arrays.asList(destKey, srcKey),
                String.valueOf(lon), String.valueOf(lat), String.valueOf(count));
    }

    public Long geoSearchStoreMeters(K destKey, K srcKey, double lon, double lat, double meters) {
        byte[] script = ("return redis.call('GEOSEARCHSTORE', KEYS[1], KEYS[2], " +
                "'FROMLONLAT', tonumber(ARGV[1]), tonumber(ARGV[2]), " +
                "'BYRADIUS', tonumber(ARGV[3]), 'm')").getBytes(StandardCharsets.UTF_8);
        return executeGeoLua(script, Arrays.asList(destKey, srcKey),
                String.valueOf(lon), String.valueOf(lat), String.valueOf(meters));
    }

    public Long geoSearchStoreMiles(K destKey, K srcKey, double lon, double lat, double miles) {
        byte[] script = "return redis.call('GEOSEARCHSTORE', KEYS[1], KEYS[2], 'FROMLONLAT', ARGV[1], ARGV[2], 'BYRADIUS', ARGV[3], 'mi')".getBytes(StandardCharsets.UTF_8);
        return executeGeoLua(script, Arrays.asList(destKey, srcKey),
                String.valueOf(lon), String.valueOf(lat), String.valueOf(miles));
    }

    @SuppressWarnings("deprecation")
    private Long executeGeoLua(byte[] script, List<K> keys, String... args) {
        byte[][] rawArgs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            rawArgs[i] = args[i].getBytes(StandardCharsets.UTF_8);
        }

        byte[][] rawKeys = new byte[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            rawKeys[i] = serializeKey(keys.get(i));
        }

        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            Object result = connection.eval(script, ReturnType.INTEGER, rawKeys.length, concatKeysAndArgs(rawKeys, rawArgs));
            return result == null ? 0L : (Long) result;
        });
    }

    private byte[][] concatKeysAndArgs(byte[][] keys, byte[][] args) {
        byte[][] result = new byte[keys.length + args.length][];
        System.arraycopy(keys, 0, result, 0, keys.length);
        System.arraycopy(args, 0, result, keys.length, args.length);
        return result;
    }

    // --- Streams ---
    public String xAdd(K key, Map<Object, Object> fields) {
        org.springframework.data.redis.connection.stream.RecordId id =
                redisTemplate.opsForStream().add(org.springframework.data.redis.connection.stream.MapRecord.create(key, fields));
        return id != null ? id.getValue() : null;
    }

    public Long xDel(K key, String... recordIds) {
        return redisTemplate.opsForStream().delete(key, recordIds);
    }

    public Long xTrim(K key, long count) {
        return redisTemplate.opsForStream().trim(key, count);
    }

    @SuppressWarnings("deprecation")
    public Long xTrimMinId(K key, String minId) {
        byte[] script = ("return redis.call('XTRIM', KEYS[1], 'MINID', ARGV[1])")
                .getBytes(StandardCharsets.UTF_8);
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            byte[] rawKey = serializeKey(key);
            byte[] rawMinId = minId.getBytes(StandardCharsets.UTF_8);
            Object result = connection.eval(script, ReturnType.INTEGER, 1, rawKey, rawMinId);
            return result == null ? 0L : (Long) result;
        });
    }

    // --- Utils ---

    public Boolean delete(K key) {
        return redisTemplate.delete(key);
    }

    public void rename(K oldKey, K newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    public Boolean renameNX(K oldKey, K newKey) {
        return redisTemplate.renameIfAbsent(oldKey, newKey);
    }

    public Boolean expire(K key, long seconds) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.keyCommands().expire(serializeKey(key), seconds));
    }

    public Boolean expireAt(K key, long unixTimestampSeconds) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.keyCommands().expireAt(serializeKey(key), unixTimestampSeconds));
    }

    public Boolean pExpire(K key, long millis) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.keyCommands().pExpire(serializeKey(key), millis));
    }

    public Boolean pExpireAt(K key, long unixTimestampMillis) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.keyCommands().pExpireAt(serializeKey(key), unixTimestampMillis));
    }

    public void expireLua(K key, long seconds, String flag) {
        String script = "return redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]), ARGV[2])";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        redisTemplate.execute(
                redisScript,
                redisTemplate.getStringSerializer(),
                new GenericToStringSerializer<>(Long.class),
                List.of(key),
                String.valueOf(seconds),
                flag.toUpperCase()
        );
    }

    public void flushDb() {
        redisTemplate.execute((RedisConnection conn) -> {
            conn.serverCommands().flushDb();
            return null;
        }, true);
    }

    public Boolean persist(K key) {
        return redisTemplate.persist(key);
    }

    public Boolean copy(K source, K destination, boolean replace) {
        return redisTemplate.copy(source, destination, replace);
    }

    @SafeVarargs
    public final Long unlink(K... keys) {
        return redisTemplate.unlink(Arrays.asList(keys));
    }

    public Boolean move(K key, int dbIndex) {
        return redisTemplate.move(key, dbIndex);
    }

    public Long pttl(K key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }

    public Long sortStore(K sourceKey, K destKey, String by, List<String> get, Integer offset, Integer count, String order, boolean alpha) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            DefaultSortParameters params = new DefaultSortParameters();

            if (by != null) {
                params.by(by.getBytes(StandardCharsets.UTF_8));
            }
            if (get != null && !get.isEmpty()) {
                for (String g : get) {
                    params.get(g.getBytes(StandardCharsets.UTF_8));
                }
            }
            if (offset != null && count != null) {
                params.limit(offset, count);
            }
            if ("DESC".equalsIgnoreCase(order)) {
                params.desc();
            }
            if (alpha) {
                params.alpha();
            }

            byte[] rawSource = serializeKey(sourceKey);
            byte[] rawDest = serializeKey(destKey);
            return connection.keyCommands().sort(rawSource, params, rawDest);
        });
    }

    public void swapDb(int db1, int db2) {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.execute("SWAPDB",
                    String.valueOf(db1).getBytes(StandardCharsets.UTF_8),
                    String.valueOf(db2).getBytes(StandardCharsets.UTF_8));
            return null;
        }, true);
    }

    @SuppressWarnings("unchecked")
    public void restoreFromObject(K key, Object value, long ttlInMillis, boolean replace) {
        K tempKey = (K) ("temp:restore:" + tempKeyCounter.incrementAndGet());
        try {
            if (value instanceof Map<?, ?> map) {
                redisTemplate.opsForHash().putAll(tempKey, map);
            } else if (value instanceof RedisZSet zset) {
                zset.forEach(entry ->
                        redisTemplate.opsForZSet().add(tempKey, (V) entry.getMember(), entry.getScore().doubleValue())
                );
            } else if (value instanceof RedisStream stream) {
                stream.forEach(entry -> {
                    MapRecord<K, Object, Object> record = MapRecord.create(tempKey, entry.getFields());
                    redisTemplate.opsForStream().add(record);
                });
            } else if (value instanceof Set<?> set) {
                redisTemplate.opsForSet().add(tempKey, (V[]) set.toArray());
            } else if (value instanceof List<?> list) {
                redisTemplate.opsForList().rightPushAll(tempKey, (V[]) list.toArray());
            } else {
                redisTemplate.opsForValue().set(tempKey, (V) value);
            }

            byte[] dump = redisTemplate.dump(tempKey);
            if (dump == null) throw new RuntimeException("Dump failed for temp key");

            redisTemplate.restore(key, dump, ttlInMillis, TimeUnit.MILLISECONDS, replace);
        } finally {
            redisTemplate.delete(tempKey);
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeKey(K key) {
        return ((RedisSerializer<K>) redisTemplate.getKeySerializer()).serialize(key);
    }

    @SuppressWarnings("unchecked")
    private V deserializeValue(byte[] bytes) {
        return ((RedisSerializer<V>) redisTemplate.getValueSerializer()).deserialize(bytes);
    }
}
