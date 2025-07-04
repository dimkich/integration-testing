package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MockInvokeResult {
    @JsonProperty("return")
    private Object return1;
    @JsonProperty("throw")
    private Throwable throw1;

    public MockInvokeResult(Object return1) {
        this.return1 = return1;
    }

    public MockInvokeResult(Throwable throw1) {
        this.throw1 = throw1;
    }
}
