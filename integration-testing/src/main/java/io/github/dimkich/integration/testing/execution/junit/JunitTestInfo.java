package io.github.dimkich.integration.testing.execution.junit;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.platform.engine.UniqueId;

@Data
@RequiredArgsConstructor
public class JunitTestInfo {
    @Getter
    private final UniqueId id;
    private final boolean isLast;
    @Getter
    @Setter
    private Integer subTestIndex;

    private boolean initialized;
    private String testFullName;

    public String getTestFullName() {
        if (testFullName == null) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                builder.append(id.getSegments().get(i).getValue().replaceAll("[^A-Za-z0-9]", "-")
                                .replace("--", "-"))
                        .append("-");
            }
            while (builder.charAt(builder.length() - 1) == '-') builder.deleteCharAt(builder.length() - 1);
            testFullName = builder.toString();
        }
        return testFullName;
    }
}
