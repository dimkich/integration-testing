package io.github.dimkich.integration.testing.redis.model;

import io.github.dimkich.integration.testing.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Total-order comparator for Redis element values used by sorted collections
 * ({@link RedisSet}, {@link RedisHash}, {@link RedisHyperLogLog}) and
 * {@link RedisZSetEntry} tie-breaking.
 * <p>
 * Numbers are compared by numeric value (via {@link BigDecimal}), {@code byte[]} values
 * by lexicographic order, and remaining types by {@link Comparable} when available;
 * otherwise by class name, hash code, and finally string representation.
 */
public class RedisDataComparator implements Comparator<Object>, Serializable {

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public int compare(Object o1, Object o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1 instanceof Number n1 && o2 instanceof Number n2) {
            return new BigDecimal(n1.toString()).compareTo(new BigDecimal(n2.toString()));
        }
        if (!o1.getClass().equals(o2.getClass())) {
            return o1.getClass().getName().compareTo(o2.getClass().getName());
        }
        if (o1 instanceof Comparable c) {
            return c.compareTo(o2);
        }
        if (o1 instanceof byte[] b1 && o2 instanceof byte[] b2) {
            return Arrays.compare(b1, b2);
        }
        int h1 = o1.hashCode();
        int h2 = o2.hashCode();
        if (h1 != h2) {
            return Integer.compare(h1, h2);
        }
        return StringUtils.objectToString(o1).compareTo(StringUtils.objectToString(o2));
    }
}