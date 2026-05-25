package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.GeoSearchStoreCommand;
import com.moilioncircle.redis.replicator.cmd.impl.UnitType;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeoSearchStoreCommandHandler implements RedisStreamHandler<GeoSearchStoreCommand> {
    private static final double EARTH_RADIUS_KM = 6372.797560856;

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == GeoSearchStoreCommand.class;
    }

    @Override
    public void handle(GeoSearchStoreCommand event, RedisInMemoryStore store) {
        final List<MemberResult> matched = new ArrayList<>();
        double[] center = new double[2];

        store.compute(event.getSource(), RedisZSet.class, (schema, srcZSet) -> {
            if (event.getFromLonLat() != null) {
                center[0] = event.getFromLonLat().getLongitude();
                center[1] = event.getFromLonLat().getLatitude();
            } else if (event.getFromMember() != null) {
                Object member = schema.getValueCodec().deserialize(event.getFromMember().getMember());
                BigDecimal score = srcZSet.getScore(member);
                if (score == null) return;
                double[] decoded = decodeGeohash(score.longValue());
                center[0] = decoded[0];
                center[1] = decoded[1];
            }

            for (RedisZSetEntry entry : srcZSet) {
                double[] pos = decodeGeohash(entry.getScore().longValue());
                double distKm = haversine(center[1], center[0], pos[1], pos[0]);

                boolean isMatch = false;
                if (event.getByRadius() != null) {
                    double rKm = toKm(event.getByRadius().getRadius(), event.getByRadius().getUnitType());
                    if (distKm <= rKm + 0.000001) isMatch = true;
                } else if (event.getByBox() != null) {
                    double wKm = toKm(event.getByBox().getWidth(), event.getByBox().getUnitType());
                    double hKm = toKm(event.getByBox().getHeight(), event.getByBox().getUnitType());
                    if (isInsideBox(center, pos, wKm, hKm)) isMatch = true;
                }

                if (isMatch) {
                    matched.add(new MemberResult(entry, distKm));
                }
            }
        });

        store.compute(event.getDestination(), RedisZSet.class, (schema, destZSet) -> {
            destZSet.clear();
            for (MemberResult res : matched) {
                BigDecimal finalScore = event.isStoreDist() ? BigDecimal.valueOf(res.dist) : res.entry.getScore();
                destZSet.put(res.entry.getMember(), finalScore);
            }
        });
    }

    private record MemberResult(RedisZSetEntry entry, double dist) {
    }

    private double toKm(double val, UnitType unit) {
        return switch (unit) {
            case M -> val / 1000.0;
            case FT -> val * 0.0003048;
            case MI -> val * 1.609344;
            default -> val;
        };
    }

    private boolean isInsideBox(double[] center, double[] pos, double wKm, double hKm) {
        double dLat = haversine(center[1], center[0], pos[1], center[0]);
        double dLon = haversine(center[1], center[0], center[1], pos[0]);
        return dLon <= (wKm / 2.0) && dLat <= (hKm / 2.0);
    }

    private double[] decodeGeohash(long geohash) {
        double[] lonRange = {-180.0, 180.0}, latRange = {-85.05112878, 85.05112878};
        for (int i = 0; i < 26; i++) {
            updateRange(lonRange, (geohash >> (51 - i * 2)) & 1);
            updateRange(latRange, (geohash >> (50 - i * 2)) & 1);
        }
        return new double[]{(lonRange[0] + lonRange[1]) / 2, (latRange[0] + latRange[1]) / 2};
    }

    private void updateRange(double[] range, long bit) {
        double mid = (range[0] + range[1]) / 2;
        if (bit == 1) range[0] = mid;
        else range[1] = mid;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}