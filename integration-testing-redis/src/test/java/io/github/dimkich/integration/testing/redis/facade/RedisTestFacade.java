package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Test-only adapter over a real Redis client used as an independent oracle: the facade writes
 * data through Jedis, while the framework reads it through its own RESP2 client.
 * <p>
 * The serializers mirror the Spring Data template this facade replaced: keys and values use
 * their own serializers, while hash fields, hash values and stream fields use the separate
 * hash serializers (JDK serialization by default, like {@code RedisTemplate}).
 */
@RequiredArgsConstructor
public class RedisTestFacade<K, V> {

    private final Jedis jedis;
    private final RedisSerializer<K> keySerializer;
    private final RedisSerializer<V> valueSerializer;
    /**
     * Hash and stream fields have their own serializers, like {@code RedisTemplate}: JDK by
     * default, strings for the facades backed by {@code StringRedisTemplate}.
     */
    private final RedisSerializer<?> hashKeySerializer;
    private final RedisSerializer<?> hashValueSerializer;
    private final AtomicLong tempKeyCounter = new AtomicLong(0);

    // --- Strings ---
    public void set(K key, V dto) {
        jedis.set(serializeKey(key), serializeValue(dto));
    }

    public V get(K key) {
        return deserializeValue(jedis.get(serializeKey(key)));
    }

    public V get(K key, int db) {
        jedis.select(db);
        try {
            return deserializeValue(jedis.get(serializeKey(key)));
        } finally {
            jedis.select(0);
        }
    }

    public Boolean setNX(K key, V value) {
        return jedis.setnx(serializeKey(key), serializeValue(value)) == 1;
    }

    public void mSet(Map<K, V> map) {
        byte[][] args = new byte[map.size() * 2][];
        int index = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            args[index++] = serializeKey(entry.getKey());
            args[index++] = serializeValue(entry.getValue());
        }
        jedis.mset(args);
    }

    public Boolean mSetNX(Map<K, V> map) {
        byte[][] args = new byte[map.size() * 2][];
        int index = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            args[index++] = serializeKey(entry.getKey());
            args[index++] = serializeValue(entry.getValue());
        }
        return jedis.msetnx(args) == 1;
    }

    public Long incrBy(K key, long delta) {
        return jedis.incrBy(serializeKey(key), delta);
    }

    public Double incrByFloat(K key, double delta) {
        return jedis.incrByFloat(serializeKey(key), delta);
    }

    public Integer append(K key, String value) {
        return (int) jedis.append(serializeKey(key), bytes(value));
    }

    public Long incr(K key) {
        return jedis.incr(serializeKey(key));
    }

    public Long decr(K key) {
        return jedis.decr(serializeKey(key));
    }

    public Long decrBy(K key, long delta) {
        return jedis.decrBy(serializeKey(key), delta);
    }

    public V getSet(K key, V value) {
        return deserializeValue(jedis.getSet(serializeKey(key), serializeValue(value)));
    }

    public void setRange(K key, long offset, V value) {
        jedis.setrange(serializeKey(key), offset, serializeValue(value));
    }

    public void setEx(K key, V value, long seconds) {
        jedis.setex(serializeKey(key), seconds, serializeValue(value));
    }

    public void pSetEx(K key, V value, long milliseconds) {
        jedis.psetex(serializeKey(key), milliseconds, serializeValue(value));
    }

    public Boolean setBit(K key, long offset, boolean value) {
        return jedis.setbit(serializeKey(key), offset, value);
    }

    public Boolean getBit(K key, long offset) {
        return jedis.getbit(serializeKey(key), offset);
    }

    @SafeVarargs
    public final Long bitOp(RedisStringCommands.BitOperation op, K destKey, K... sourceKeys) {
        byte[][] rawSourceKeys = new byte[sourceKeys.length][];
        for (int i = 0; i < sourceKeys.length; i++) {
            rawSourceKeys[i] = serializeKey(sourceKeys[i]);
        }
        return jedis.bitop(BitOP.valueOf(op.name()), serializeKey(destKey), rawSourceKeys);
    }

    public List<Long> bitField(K key, BitFieldSubCommands subCommands) {
        return jedis.bitfield(serializeKey(key), bitFieldArgs(subCommands));
    }

    @SafeVarargs
    public final Long pfAdd(K key, V... elements) {
        return jedis.pfadd(serializeKey(key), rawValues(elements));
    }

    @SafeVarargs
    public final Long pfCount(K... keys) {
        byte[][] rawKeys = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            rawKeys[i] = serializeKey(keys[i]);
        }
        return jedis.pfcount(rawKeys);
    }

    /**
     * {@code PFMERGE} itself replies with {@code OK}; the returned count is a follow-up
     * {@code PFCOUNT}, matching the behavior of the Spring Data API this facade replaced.
     */
    @SafeVarargs
    public final Long pfMerge(K destKey, K... sourceKeys) {
        byte[] rawDestKey = serializeKey(destKey);
        jedis.pfmerge(rawDestKey, rawKeysArray(sourceKeys));
        return jedis.pfcount(rawDestKey);
    }

    // --- Hashes ---
    public <HK> void hSet(K key, HK hashKey, V dto) {
        jedis.hset(serializeKey(key), serializeHashKey(hashKey), serializeHashValue(dto));
    }

    public <HK> V hGet(K key, HK hashKey) {
        return deserializeHashValue(jedis.hget(serializeKey(key), serializeHashKey(hashKey)));
    }

    /**
     * {@code HGETALL} is sent verbatim because {@code Jedis.hgetAll} collects pairs into a
     * {@code HashMap}, while the XML expectations are ordered maps: the raw reply keeps the
     * server order.
     */
    @SuppressWarnings("unchecked")
    public <HK> Map<HK, V> hGetAll(K key) {
        Map<HK, V> result = new LinkedHashMap<>();
        Object reply = jedis.sendCommand(Protocol.Command.HGETALL, serializeKey(key));
        if (reply instanceof List<?> flat) {
            for (int i = 0; i + 1 < flat.size(); i += 2) {
                byte[] field = (byte[]) flat.get(i);
                byte[] value = (byte[]) flat.get(i + 1);
                result.put((HK) hashKeySerializer.deserialize(field), deserializeHashValue(value));
            }
        }
        return result;
    }

    public <HK> Long hDel(K key, HK hashKeys) {
        return jedis.hdel(serializeKey(key), serializeHashKey(hashKeys));
    }

    public <HK> Boolean hSetNx(K key, HK hashKey, V value) {
        return jedis.hsetnx(serializeKey(key), serializeHashKey(hashKey), serializeHashValue(value)) == 1;
    }

    public <HK> Long hIncrBy(K key, HK hashKey, long delta) {
        return jedis.hincrBy(serializeKey(key), serializeHashKey(hashKey), delta);
    }

    public Double hIncrByFloat(K key, Object hashKey, double delta) {
        return jedis.hincrByFloat(serializeKey(key), serializeHashKey(hashKey), delta);
    }

    public void hMSet(K key, Map<Object, V> map) {
        Map<byte[], byte[]> fields = new LinkedHashMap<>();
        map.forEach((field, value) -> fields.put(serializeHashKey(field), serializeHashValue(value)));
        jedis.hset(serializeKey(key), fields);
    }

    /**
     * {@code HEXPIRE} is a Redis 7.4 command unknown to Jedis 4.3, so it is sent verbatim.
     */
    public void hExpire(K key, long seconds, String flag, String[] fields) {
        List<String> leading = new ArrayList<>(2);
        leading.add(String.valueOf(seconds));
        if (flag != null && !flag.isBlank()) {
            leading.add(flag.toUpperCase(Locale.ROOT).trim());
        }
        jedis.sendCommand(() -> bytes("HEXPIRE"), hashFieldArgs(key, fields, leading));
    }

    /**
     * {@code HPERSIST} is a Redis 7.4 command unknown to Jedis 4.3, so it is sent verbatim.
     */
    public void hPersist(K key, String[] fields) {
        jedis.sendCommand(() -> bytes("HPERSIST"), hashFieldArgs(key, fields));
    }

    /**
     * {@code HTTL} is a Redis 7.4 command unknown to Jedis 4.3, so it is sent verbatim.
     */
    public List<Long> hTtl(K key, String[] fields) {
        Object reply = jedis.sendCommand(() -> bytes("HTTL"), hashFieldArgs(key, fields));
        List<Long> ttls = new ArrayList<>();
        if (reply instanceof List<?> values) {
            for (Object value : values) {
                ttls.add(((Number) value).longValue());
            }
        }
        return ttls;
    }

    /**
     * Builds the {@code key [leading...] FIELDS <count> <field...>} argument list shared by the
     * hash field TTL commands.
     */
    private byte[][] hashFieldArgs(K key, String[] fields) {
        return hashFieldArgs(key, fields, List.of());
    }

    private byte[][] hashFieldArgs(K key, String[] fields, List<String> leading) {
        List<byte[]> args = new ArrayList<>(fields.length + leading.size() + 3);
        args.add(serializeKey(key));
        for (String arg : leading) {
            args.add(bytes(arg));
        }
        args.add(bytes("FIELDS"));
        args.add(bytes(String.valueOf(fields.length)));
        for (String field : fields) {
            args.add(bytes(field));
        }
        return args.toArray(new byte[0][]);
    }

    // --- Lists ---
    public List<V> lRange(K key, long start, long end) {
        return values(jedis.lrange(serializeKey(key), start, end));
    }

    @SafeVarargs
    public final Long lPush(K key, V... values) {
        return jedis.lpush(serializeKey(key), rawValues(values));
    }

    @SafeVarargs
    public final Long rPush(K key, V... values) {
        return jedis.rpush(serializeKey(key), rawValues(values));
    }

    public V lPop(K key) {
        return deserializeValue(jedis.lpop(serializeKey(key)));
    }

    public List<V> lPop(K key, long count) {
        return values(jedis.lpop(serializeKey(key), (int) count));
    }

    public V rPop(K key) {
        return deserializeValue(jedis.rpop(serializeKey(key)));
    }

    public List<V> rPop(K key, long count) {
        return values(jedis.rpop(serializeKey(key), (int) count));
    }

    public void lSet(K key, long index, V value) {
        translate(() -> jedis.lset(serializeKey(key), index, serializeValue(value)));
    }

    public Long lRem(K key, long count, V value) {
        return jedis.lrem(serializeKey(key), count, serializeValue(value));
    }

    public void lTrim(K key, long start, long stop) {
        jedis.ltrim(serializeKey(key), start, stop);
    }

    public Long lInsertBefore(K key, V pivot, V value) {
        return jedis.linsert(serializeKey(key), ListPosition.BEFORE, serializeValue(pivot), serializeValue(value));
    }

    public Long lInsertAfter(K key, V pivot, V value) {
        return jedis.linsert(serializeKey(key), ListPosition.AFTER, serializeValue(pivot), serializeValue(value));
    }

    public V lMove(K sourceKey, K destKey, RedisListCommands.Direction from, RedisListCommands.Direction to) {
        return deserializeValue(jedis.lmove(serializeKey(sourceKey), serializeKey(destKey),
                ListDirection.valueOf(from.name()), ListDirection.valueOf(to.name())));
    }

    public Long lPushX(K key, V value) {
        return jedis.lpushx(serializeKey(key), serializeValue(value));
    }

    public Long rPushX(K key, V value) {
        return jedis.rpushx(serializeKey(key), serializeValue(value));
    }

    public V rPopLPush(K source, K destination) {
        return deserializeValue(jedis.rpoplpush(serializeKey(source), serializeKey(destination)));
    }

    public V brPopLPush(K source, K destination, long timeoutSeconds) {
        return deserializeValue(jedis.brpoplpush(serializeKey(source), serializeKey(destination),
                (int) timeoutSeconds));
    }

    public V blMove(K sourceKey, K destKey, RedisListCommands.Direction from, RedisListCommands.Direction to,
                    long timeoutSeconds) {
        return deserializeValue(jedis.blmove(serializeKey(sourceKey), serializeKey(destKey),
                ListDirection.valueOf(from.name()), ListDirection.valueOf(to.name()), timeoutSeconds));
    }

    // --- Sets ---
    @SafeVarargs
    public final Long sAdd(K key, V... values) {
        return jedis.sadd(serializeKey(key), rawValues(values));
    }

    @SafeVarargs
    public final Long sRem(K key, V... values) {
        return jedis.srem(serializeKey(key), rawValues(values));
    }

    public Boolean sMove(K sourceKey, V member, K destinationKey) {
        return jedis.smove(serializeKey(sourceKey), serializeKey(destinationKey), serializeValue(member)) == 1;
    }

    public Long sUnionStore(K key, Collection<K> otherKeys, K destinationKey) {
        return jedis.sunionstore(serializeKey(destinationKey), rawKeys(key, otherKeys));
    }

    public Long sInterStore(K key, Collection<K> otherKeys, K destinationKey) {
        return jedis.sinterstore(serializeKey(destinationKey), rawKeys(key, otherKeys));
    }

    public Long sDiffStore(K key, Collection<K> otherKeys, K destinationKey) {
        return jedis.sdiffstore(serializeKey(destinationKey), rawKeys(key, otherKeys));
    }

    // --- Sorted sets ---
    public Boolean zAdd(K key, V value, double score) {
        return jedis.zadd(serializeKey(key), score, serializeValue(value)) > 0;
    }

    public Boolean zAddNX(K key, V value, double score) {
        return zAddInternal(key, value, score, ZAddParams.zAddParams().nx());
    }

    public Boolean zAddXX(K key, V value, double score) {
        return zAddInternal(key, value, score, ZAddParams.zAddParams().xx());
    }

    public Boolean zAddGT(K key, V value, double score) {
        return zAddInternal(key, value, score, ZAddParams.zAddParams().gt());
    }

    public Boolean zAddLT(K key, V value, double score) {
        return zAddInternal(key, value, score, ZAddParams.zAddParams().lt());
    }

    private Boolean zAddInternal(K key, V value, double score, ZAddParams params) {
        return jedis.zadd(serializeKey(key), score, serializeValue(value), params) > 0;
    }

    public Double zIncrBy(K key, V value, double delta) {
        return jedis.zincrby(serializeKey(key), delta, serializeValue(value));
    }

    @SafeVarargs
    public final Long zRem(K key, V... values) {
        return jedis.zrem(serializeKey(key), rawValues(values));
    }

    public Long zRemRangeByRank(K key, long start, long stop) {
        return jedis.zremrangeByRank(serializeKey(key), start, stop);
    }

    public Long zRemRangeByScore(K key, String minStr, String maxStr) {
        return jedis.zremrangeByScore(serializeKey(key), bytes(minStr), bytes(maxStr));
    }

    public V zPopMax(K key) {
        return zPopSingle(jedis.zpopmax(serializeKey(key)));
    }

    public Set<V> zPopMax(K key, long count) {
        return zPopMany(serializeKey(key), count, true);
    }

    public V zPopMin(K key) {
        return zPopSingle(jedis.zpopmin(serializeKey(key)));
    }

    public Set<V> zPopMin(K key, long count) {
        return zPopMany(serializeKey(key), count, false);
    }

    private V zPopSingle(Tuple tuple) {
        return tuple == null ? null : deserializeValue(tuple.getBinaryElement());
    }

    private Set<V> zPopMany(byte[] rawKey, long count, boolean max) {
        List<Tuple> tuples = max ? jedis.zpopmax(rawKey, (int) count) : jedis.zpopmin(rawKey, (int) count);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptySet();
        }
        Set<V> result = new LinkedHashSet<>();
        for (Tuple tuple : tuples) {
            result.add(deserializeValue(tuple.getBinaryElement()));
        }
        return result;
    }

    public Long zRemRangeByLex(K key, String min, String max) {
        return jedis.zremrangeByLex(serializeKey(key), lexBound(min), lexBound(max));
    }

    public Long zUnionStore(K key, Collection<K> otherKeys, K destKey) {
        return jedis.zunionstore(serializeKey(destKey), rawKeys(key, otherKeys));
    }

    public Long zUnionStore(K key, List<K> otherKeys, K destKey, Aggregate aggregate, double[] weights) {
        ZParams params = new ZParams()
                .aggregate(ZParams.Aggregate.valueOf(aggregate.name()))
                .weights(weights);
        return jedis.zunionstore(serializeKey(destKey), params, rawKeys(key, otherKeys));
    }

    public Long zInterStore(K key, List<K> otherKeys, K destKey) {
        return jedis.zinterstore(serializeKey(destKey), rawKeys(key, otherKeys));
    }

    public Long zInterStore(K key, List<K> otherKeys, K destKey, Aggregate aggregate, double[] weights) {
        ZParams params = new ZParams()
                .aggregate(ZParams.Aggregate.valueOf(aggregate.name()))
                .weights(weights);
        return jedis.zinterstore(serializeKey(destKey), params, rawKeys(key, otherKeys));
    }

    public Long zDiffStore(K key, List<K> otherKeys, K destKey) {
        return jedis.zdiffStore(serializeKey(destKey), rawKeys(key, otherKeys));
    }

    public Long zRangeStoreByScore(K srcKey, K destKey, Double min, Double max) {
        return jedis.zrangestore(serializeKey(destKey), serializeKey(srcKey),
                ZRangeParams.zrangeByScoreParams(min, max));
    }

    public Long zRangeStore(K srcKey, K destKey, long start, long stop) {
        return jedis.zrangestore(serializeKey(destKey), serializeKey(srcKey),
                new ZRangeParams((int) start, (int) stop));
    }

    public Long zRangeStoreByLex(K srcKey, K destKey, String min, String max) {
        return jedis.zrangestore(serializeKey(destKey), serializeKey(srcKey),
                new ZRangeParams(Protocol.Keyword.BYLEX, lexBound(min), lexBound(max)));
    }

    /**
     * Redis expects the {@code max} bound first when {@code REV} is used, so the bounds are
     * deliberately passed in reverse order.
     */
    public Long zRangeStoreByScoreRev(K srcKey, K destKey, Double min, Double max, long offset, long count) {
        return jedis.zrangestore(serializeKey(destKey), serializeKey(srcKey),
                new ZRangeParams(Protocol.Keyword.BYSCORE, bytes(String.valueOf(max)), bytes(String.valueOf(min)))
                        .rev()
                        .limit((int) offset, (int) count));
    }

    public Long zRangeStoreByScoreRaw(K srcKey, K destKey, String min, String max, boolean rev) {
        ZRangeParams params = new ZRangeParams(Protocol.Keyword.BYSCORE,
                bytes(rev ? max : min), bytes(rev ? min : max));
        if (rev) {
            params.rev();
        }
        return jedis.zrangestore(serializeKey(destKey), serializeKey(srcKey), params);
    }

    // --- Geo ---
    public Long geoAdd(K key, double lon, double lat, V member) {
        return jedis.geoadd(serializeKey(key), lon, lat, serializeValue(member));
    }

    public Long geoSearchStore(K destKey, K srcKey, double lon, double lat, double radius) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byRadius(radius, GeoUnit.KM));
    }

    public Long geoSearchStoreBox(K destKey, K srcKey, double lon, double lat, double width, double height) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byBox(width, height, GeoUnit.KM));
    }

    public Long geoSearchStoreLimit(K destKey, K srcKey, double lon, double lat, int count) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byRadius(100, GeoUnit.KM)
                .asc()
                .count(count));
    }

    public Long geoSearchStoreMeters(K destKey, K srcKey, double lon, double lat, double meters) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byRadius(meters, GeoUnit.M));
    }

    public Long geoSearchStoreMiles(K destKey, K srcKey, double lon, double lat, double miles) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byRadius(miles, GeoUnit.MI));
    }

    public Long geoSearchStoreUnit(K destKey, K srcKey, double lon, double lat, double radius, String unit) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromLonLat(lon, lat)
                .byRadius(radius, GeoUnit.valueOf(unit.toUpperCase(Locale.ROOT))));
    }

    public Long geoSearchStoreByMember(K destKey, K srcKey, V member, double radius) {
        return geoStore(destKey, srcKey, GeoSearchParam.geoSearchParam()
                .fromMember(String.valueOf(member))
                .byRadius(radius, GeoUnit.KM));
    }

    public Long geoSearchStoreByMemberStoreDist(K destKey, K srcKey, V member, double radius) {
        return jedis.geosearchStoreStoreDist(serializeKey(destKey), serializeKey(srcKey),
                GeoSearchParam.geoSearchParam()
                        .fromMember(String.valueOf(member))
                        .byRadius(radius, GeoUnit.KM));
    }

    private Long geoStore(K destKey, K srcKey, GeoSearchParam params) {
        return jedis.geosearchStore(serializeKey(destKey), serializeKey(srcKey), params);
    }

    // --- Streams ---
    public String xAdd(K key, Map<Object, Object> fields) {
        byte[] id = jedis.xadd(serializeKey(key), XAddParams.xAddParams(), rawFields(fields));
        return id == null ? null : new String(id, StandardCharsets.UTF_8);
    }
    public Long xDel(K key, String... recordIds) {
        return jedis.xdel(serializeKey(key), rawStrings(recordIds));
    }

    public Long xTrim(K key, long count) {
        return jedis.xtrim(serializeKey(key), count, false);
    }

    public Long xTrimMinId(K key, String minId) {
        return jedis.xtrim(serializeKey(key), XTrimParams.xTrimParams().minId(minId));
    }

    // --- Keys ---
    public Boolean delete(K key) {
        return jedis.del(serializeKey(key)) == 1;
    }

    public void rename(K oldKey, K newKey) {
        translate(() -> jedis.rename(serializeKey(oldKey), serializeKey(newKey)));
    }

    public Boolean renameNX(K oldKey, K newKey) {
        return translate(() -> jedis.renamenx(serializeKey(oldKey), serializeKey(newKey)) == 1);
    }

    public Boolean expire(K key, long seconds) {
        return jedis.expire(serializeKey(key), seconds) == 1;
    }

    public Boolean expireAt(K key, long unixTimestampSeconds) {
        return jedis.expireAt(serializeKey(key), unixTimestampSeconds) == 1;
    }

    public Boolean pExpire(K key, long millis) {
        return jedis.pexpire(serializeKey(key), millis) == 1;
    }

    public Boolean pExpireAt(K key, long unixTimestampMillis) {
        return jedis.pexpireAt(serializeKey(key), unixTimestampMillis) == 1;
    }

    public void expireLua(K key, long seconds, String flag) {
        jedis.expire(serializeKey(key), seconds, ExpiryOption.valueOf(flag.toUpperCase(Locale.ROOT)));
    }

    public void flushDb() {
        jedis.flushDB();
    }

    public Boolean persist(K key) {
        return jedis.persist(serializeKey(key)) == 1;
    }

    public Boolean copy(K source, K destination, boolean replace) {
        return jedis.copy(serializeKey(source), serializeKey(destination), replace);
    }

    @SafeVarargs
    public final Long unlink(K... keys) {
        byte[][] rawKeys = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            rawKeys[i] = serializeKey(keys[i]);
        }
        return jedis.unlink(rawKeys);
    }

    public Boolean move(K key, int dbIndex) {
        return jedis.move(serializeKey(key), dbIndex) == 1;
    }

    public Long pttl(K key) {
        return jedis.pttl(serializeKey(key));
    }

    public Long sortStore(K sourceKey, K destKey, String by, List<String> get, Integer offset, Integer count,
                          String order, boolean alpha) {
        SortingParams params = new SortingParams();
        if (by != null) {
            params.by(bytes(by));
        }
        if (get != null && !get.isEmpty()) {
            byte[][] patterns = new byte[get.size()][];
            for (int i = 0; i < get.size(); i++) {
                patterns[i] = bytes(get.get(i));
            }
            params.get(patterns);
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
        return jedis.sort(serializeKey(sourceKey), params, serializeKey(destKey));
    }

    public void swapDb(int db1, int db2) {
        jedis.swapDB(db1, db2);
    }

    // --- Restore ---
    public void restoreFromObject(K key, Object value, long ttlInMillis, boolean replace) {
        RestoreParams params = RestoreParams.restoreParams();
        if (replace) {
            params.replace();
        }
        restore(key, value, ttlInMillis, params);
    }

    public void restoreFromObjectWithOptions(K key, Object value, long ttlInMillis) {
        restoreFromObjectWithOptions(key, value, ttlInMillis, new String[0]);
    }

    public void restoreFromObjectWithOptions(K key, Object value, long ttlInMillis, String... options) {
        restore(key, value, ttlInMillis, restoreParams(options));
    }

    private void restore(K key, Object value, long ttlInMillis, RestoreParams params) {
        K tempKey = tempKey();
        try {
            populateTempKey(tempKey, value);
            byte[] dump = jedis.dump(serializeKey(tempKey));
            if (dump == null) {
                throw new RuntimeException("Dump failed for temp key");
            }
            translate(() -> jedis.restore(serializeKey(key), ttlInMillis, dump, params));
        } finally {
            jedis.del(serializeKey(tempKey));
        }
    }

    /**
     * {@code SET} with options. {@code GET} is sent through {@code setGet}: Jedis deprecated
     * {@code SetParams.get()} in its favor, and only {@code setGet} returns the old value.
     */
    public String setWithOptions(K key, V value, String... options) {
        SetParams params = SetParams.setParams();
        boolean returnOldValue = false;
        for (int i = 0; i < options.length; i++) {
            String option = options[i].toUpperCase(Locale.ROOT);
            switch (option) {
                case "EX" -> params.ex(Long.parseLong(options[++i]));
                case "PX" -> params.px(Long.parseLong(options[++i]));
                case "EXAT" -> params.exAt(Long.parseLong(options[++i]));
                case "PXAT" -> params.pxAt(Long.parseLong(options[++i]));
                case "NX" -> params.nx();
                case "XX" -> params.xx();
                case "KEEPTTL" -> params.keepttl();
                case "GET" -> returnOldValue = true;
                default -> throw new IllegalArgumentException("Unsupported SET option: " + options[i]);
            }
        }
        if (returnOldValue) {
            byte[] oldValue = jedis.setGet(serializeKey(key), serializeValue(value), params);
            return oldValue == null ? null : new String(oldValue, StandardCharsets.UTF_8);
        }
        return jedis.set(serializeKey(key), serializeValue(value), params);
    }

    public String xAddNoMkStream(K key, String id, Map<?, ?> fields) {
        XAddParams params = XAddParams.xAddParams().id(id).noMkStream();
        byte[] entryId = jedis.xadd(serializeKey(key), params, rawFields(fields));
        return entryId == null ? null : new String(entryId, StandardCharsets.UTF_8);
    }

    // --- Internals ---
    private void populateTempKey(K tempKey, Object value) {
        if (value instanceof Map<?, ?> map) {
            jedis.hset(serializeKey(tempKey), rawFields(map));
        } else if (value instanceof RedisZSet zset) {
            zset.forEach(entry -> jedis.zadd(serializeKey(tempKey), entry.getScore().doubleValue(),
                    serializeValue(entry.getMember())));
        } else if (value instanceof RedisStream stream) {
            stream.forEach(entry -> jedis.xadd(serializeKey(tempKey), XAddParams.xAddParams(),
                    rawFields(entry.getFields())));
        } else if (value instanceof Set<?> set) {
            for (Object item : set) {
                jedis.sadd(serializeKey(tempKey), serializeValue(item));
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                jedis.rpush(serializeKey(tempKey), serializeValue(item));
            }
        } else {
            jedis.set(serializeKey(tempKey), serializeValue(value));
        }
    }

    @SuppressWarnings("unchecked")
    private K tempKey() {
        return (K) ("temp:restore:" + tempKeyCounter.incrementAndGet());
    }

    private static RestoreParams restoreParams(String... options) {
        RestoreParams params = RestoreParams.restoreParams();
        for (int i = 0; i < options.length; i++) {
            String option = options[i].toUpperCase(Locale.ROOT);
            switch (option) {
                case "REPLACE" -> params.replace();
                case "ABSTTL" -> params.absTtl();
                case "IDLETIME" -> params.idleTime(Long.parseLong(options[++i]));
                case "FREQ" -> params.frequency(Long.parseLong(options[++i]));
                default -> throw new IllegalArgumentException("Unsupported RESTORE option: " + options[i]);
            }
        }
        return params;
    }

    private Map<byte[], byte[]> rawFields(Map<?, ?> fields) {
        Map<byte[], byte[]> rawFields = new LinkedHashMap<>();
        fields.forEach((field, value) -> rawFields.put(serializeHashKey(field), serializeHashValue(value)));
        return rawFields;
    }

    private byte[][] rawValues(V[] values) {
        byte[][] rawValues = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            rawValues[i] = serializeValue(values[i]);
        }
        return rawValues;
    }

    private byte[][] rawKeysArray(K[] keys) {
        byte[][] rawKeys = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            rawKeys[i] = serializeKey(keys[i]);
        }
        return rawKeys;
    }

    private byte[][] rawKeys(K key, Collection<K> otherKeys) {
        byte[][] rawKeys = new byte[otherKeys.size() + 1][];
        rawKeys[0] = serializeKey(key);
        int index = 1;
        for (K otherKey : otherKeys) {
            rawKeys[index++] = serializeKey(otherKey);
        }
        return rawKeys;
    }

    private static byte[][] rawStrings(String[] values) {
        byte[][] rawValues = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            rawValues[i] = bytes(values[i]);
        }
        return rawValues;
    }

    private List<V> values(List<byte[]> rawValues) {
        List<V> result = new ArrayList<>(rawValues.size());
        for (byte[] rawValue : rawValues) {
            result.add(deserializeValue(rawValue));
        }
        return result;
    }

    /**
     * Renders subcommands exactly like {@code RedissonConnection}: {@code OVERFLOW} follows the
     * {@code INCRBY} it belongs to. Test expectations were recorded against that ordering.
     */
    private static byte[][] bitFieldArgs(BitFieldSubCommands subCommands) {
        List<byte[]> args = new ArrayList<>();
        for (BitFieldSubCommands.BitFieldSubCommand subCommand : subCommands) {
            args.add(bytes(subCommand.getCommand()));
            args.add(bytes(subCommand.getType().asString()));
            args.add(bytes(subCommand.getOffset().asString()));
            if (subCommand instanceof BitFieldSubCommands.BitFieldSet bitFieldSet) {
                args.add(bytes(String.valueOf(bitFieldSet.getValue())));
            } else if (subCommand instanceof BitFieldSubCommands.BitFieldIncrBy bitFieldIncrBy) {
                args.add(bytes(String.valueOf(bitFieldIncrBy.getValue())));
                if (bitFieldIncrBy.getOverflow() != null) {
                    args.add(bytes("OVERFLOW"));
                    args.add(bytes(bitFieldIncrBy.getOverflow().name()));
                }
            }
        }
        return args.toArray(new byte[0][]);
    }

    /**
     * Translates a server error the same way Spring Data did, so XML error expectations stay
     * unchanged.
     */
    private static <T> T translate(Supplier<T> command) {
        try {
            return command.get();
        } catch (JedisDataException e) {
            throw new InvalidDataAccessApiUsageException(e.getMessage(), e);
        }
    }

    private byte[] serializeKey(K key) {
        return keySerializer.serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeValue(Object value) {
        return ((RedisSerializer<Object>) valueSerializer).serialize(value);
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeHashKey(Object hashKey) {
        return ((RedisSerializer<Object>) hashKeySerializer).serialize(hashKey);
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeHashValue(Object value) {
        return ((RedisSerializer<Object>) hashValueSerializer).serialize(value);
    }

    @SuppressWarnings("unchecked")
    private V deserializeHashValue(byte[] bytes) {
        return bytes == null ? null : (V) hashValueSerializer.deserialize(bytes);
    }

    private V deserializeValue(byte[] bytes) {
        return bytes == null ? null : valueSerializer.deserialize(bytes);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Lex bounds must carry an explicit {@code [} or {@code (} prefix; bare strings are
     * treated as inclusive, matching the Spring Data behavior this facade replaced.
     */
    private static byte[] lexBound(String bound) {
        if (bound.startsWith("[") || bound.startsWith("(") || "-".equals(bound) || "+".equals(bound)) {
            return bytes(bound);
        }
        return bytes("[" + bound);
    }
}
