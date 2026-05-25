package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.AbstractCommand;
import com.moilioncircle.redis.replicator.cmd.impl.Limit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class ZRangeStoreCommand extends AbstractCommand {
    private final byte[] dest;
    private final byte[] src;
    private final byte[] min;
    private final byte[] max;
    private final RangeType rangeType;
    private final Limit limit;
    private final boolean rev;

    public enum RangeType {
        RANK, SCORE, LEX
    }
}