package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.*;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class BitFieldCommandHandler implements RedisStreamHandler<BitFieldCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == BitFieldCommand.class;
    }

    @Override
    public void handle(BitFieldCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            byte[] bytes = getBytes(entry.getData(), schema);
            bytes = applyStatementList(bytes, event.getStatements(), OverFlowType.WRAP);

            if (event.getOverFlows() != null) {
                for (OverFlow of : event.getOverFlows()) {
                    bytes = applyStatementList(bytes, of.getStatements(), of.getOverFlowType());
                }
            }
            entry.setData(bytes);
        });
    }

    private byte[] applyStatementList(byte[] bytes, List<Statement> statements, OverFlowType strategy) {
        if (statements == null) return bytes;

        for (Statement stmt : statements) {
            if (stmt instanceof SetTypeOffsetValue s) {
                int width = getWidth(s.getType());
                bytes = writeBits(bytes, parseOffset(s.getOffset(), width), width, s.getValue());
            } else if (stmt instanceof IncrByTypeOffsetIncrement i) {
                int width = getWidth(i.getType());
                boolean signed = isSigned(i.getType());
                long offset = parseOffset(i.getOffset(), width);

                long currentVal = readBits(bytes, offset, width, signed);
                Long newVal = calculateNewValue(currentVal, i.getIncrement(), width, signed, strategy);

                if (newVal != null) {
                    bytes = writeBits(bytes, offset, width, newVal);
                }
            }
        }
        return bytes;
    }

    private Long calculateNewValue(long current, long incr, int width, boolean signed, OverFlowType strategy) {
        if (strategy == OverFlowType.FAIL) {
            try {
                long res = Math.addExact(current, incr);
                if (isBeyondWidth(res, width, signed)) {
                    return null;
                }
                return res;
            } catch (ArithmeticException e) {
                return null;
            }
        }

        long res = current + incr;
        if (strategy == OverFlowType.SAT) {
            long max = signed ? (width == 64 ? Long.MAX_VALUE : (1L << (width - 1)) - 1)
                    : (1L << width) - 1;
            long min = signed ? (width == 64 ? Long.MIN_VALUE : -(1L << (width - 1)))
                    : 0;
            if (res > max) return max;
            if (res < min) return min;
        }

        return applyWrap(res, width, signed);
    }

    private boolean isBeyondWidth(long val, int width, boolean signed) {
        if (width >= 64) return false;
        if (signed) {
            long max = (1L << (width - 1)) - 1;
            long min = -(1L << (width - 1));
            return val > max || val < min;
        }
        long max = (1L << width) - 1;
        return val > max || val < 0;
    }

    private byte[] writeBits(byte[] bytes, long bitOffset, int width, long value) {
        int requiredBytes = (int) ((bitOffset + width + 7) / 8);
        if (bytes.length < requiredBytes) bytes = Arrays.copyOf(bytes, requiredBytes);
        for (int i = 0; i < width; i++) {
            long bitPos = bitOffset + width - 1 - i;
            int byteIdx = (int) (bitPos / 8);
            int bitIdx = 7 - (int) (bitPos % 8);
            if (((value >> i) & 1) == 1) bytes[byteIdx] |= (byte) (1 << bitIdx);
            else bytes[byteIdx] &= (byte) ~(1 << bitIdx);
        }
        return bytes;
    }

    private long readBits(byte[] bytes, long bitOffset, int width, boolean signed) {
        long val = 0;
        for (int i = 0; i < width; i++) {
            long currentBitPos = bitOffset + i;
            int byteIdx = (int) (currentBitPos / 8);
            int bitIdx = 7 - (int) (currentBitPos % 8);
            val <<= 1;
            if (byteIdx < bytes.length && ((bytes[byteIdx] >> bitIdx) & 1) == 1) val |= 1;
        }
        if (signed && width < 64 && (val & (1L << (width - 1))) != 0) {
            val |= -(1L << width);
        }
        return val;
    }

    private long applyWrap(long val, int width, boolean signed) {
        if (width >= 64) return val;
        long mask = (1L << width) - 1;
        long res = val & mask;
        if (signed && (res & (1L << (width - 1))) != 0) res |= ~mask;
        return res;
    }

    private long parseOffset(byte[] offsetBytes, int width) {
        String s = new String(offsetBytes, StandardCharsets.UTF_8);
        return s.startsWith("#") ? Long.parseLong(s.substring(1)) * width : Long.parseLong(s);
    }

    private int getWidth(byte[] type) {
        return Integer.parseInt(new String(type, StandardCharsets.UTF_8).substring(1));
    }

    private boolean isSigned(byte[] type) {
        return type[0] == 'i';
    }

    private byte[] getBytes(Object data, RedisDataSchema schema) {
        if (data instanceof byte[]) return (byte[]) data;
        return (data == null) ? new byte[0] : schema.getValueCodec().serialize(data);
    }
}