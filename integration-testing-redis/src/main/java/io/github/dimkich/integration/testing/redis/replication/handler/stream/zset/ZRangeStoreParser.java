package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.Limit;
import io.github.dimkich.integration.testing.redis.replication.NamedCommandParser;
import org.springframework.stereotype.Component;

@Component
public class ZRangeStoreParser implements NamedCommandParser<ZRangeStoreCommand> {

    @Override
    public String getCommandName() {
        return "ZRANGESTORE";
    }

    @Override
    public ZRangeStoreCommand parse(Object[] command) {
        int i = 1;
        byte[] dest = (byte[]) command[i++];
        byte[] src = (byte[]) command[i++];
        byte[] min = (byte[]) command[i++];
        byte[] max = (byte[]) command[i++];

        ZRangeStoreCommand.RangeType rangeType = ZRangeStoreCommand.RangeType.RANK;
        boolean rev = false;
        Limit limit = null;

        while (i < command.length) {
            String param = new String((byte[]) command[i++]).toUpperCase();
            switch (param) {
                case "BYSCORE" -> rangeType = ZRangeStoreCommand.RangeType.SCORE;
                case "BYLEX" -> rangeType = ZRangeStoreCommand.RangeType.LEX;
                case "REV" -> rev = true;
                case "LIMIT" -> {
                    long offset = Long.parseLong(new String((byte[]) command[i++]));
                    long count = Long.parseLong(new String((byte[]) command[i++]));
                    limit = new Limit(offset, count);
                }
            }
        }
        return new ZRangeStoreCommand(dest, src, min, max, rangeType, limit, rev);
    }
}