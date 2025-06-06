package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class MockInvoke {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private String method;
    private List<Object> arg;
    private Object result;
    private Throwable exception;

    public boolean equalsTo(String name, String method, List<Object> arg) {
        return Objects.equals(this.name, name) && Objects.equals(this.method, method) && Objects.equals(this.arg, arg);
    }
}
