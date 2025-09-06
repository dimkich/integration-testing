package io.github.dimkich.integration.testing.execution.junit;

import lombok.Data;
import org.junit.platform.engine.UniqueId;

import java.io.File;
import java.util.List;

@Data
public class JunitTestInfo {
    private final UniqueId id;
    private final Integer index;
    private final boolean isLast;

    private boolean initialized;

    public JunitTestInfo(UniqueId id, boolean isLast) {
        this.id = id;
        this.isLast = isLast;
        List<UniqueId.Segment> segments = id.getSegments();
        UniqueId.Segment segment = segments.get(segments.size() - 1);
        if (segment.getValue().startsWith("#")) {
            index = Integer.parseInt(segment.getValue().substring(1)) - 1;
        } else {
            index = null;
        }
    }

    public String getTestFilePath() {
        List<UniqueId.Segment> segments = id.getSegments();
        return segments.get(1).getValue() + File.separator + segments.get(2).getValue();
    }
}
