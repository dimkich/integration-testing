package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicMarkableReference;

@Data
@Accessors(chain = true)
public class MockInvoke {
    private static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    private static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    static {
        compConfig.registerEqualsForType((amr1, amr2) -> compCalculator.determineDifferences(amr1.getReference(),
                amr2.getReference(), compConfig).isEmpty(), AtomicMarkableReference.class);
    }

    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private String method;
    private List<Object> arg;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MockInvokeResult> result;
    @JsonIgnore
    private int resultIndex = 0;

    public void addResult(Object result) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new MockInvokeResult(result));
    }

    public void addException(Throwable e) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new MockInvokeResult(e));
    }

    @JsonIgnore
    public Object getCurrentResult() {
        if (this.result == null) {
            return null;
        }
        MockInvokeResult r = result.get(resultIndex % result.size());
        resultIndex++;
        return r.getReturn1();
    }

    @JsonIgnore
    public void tryThrowException() throws Throwable {
        if (this.result == null) {
            return;
        }
        MockInvokeResult r = result.get(resultIndex % result.size());
        if (r.getThrow1() != null) {
            resultIndex++;
            throw r.getThrow1();
        }
    }

    @SneakyThrows
    public boolean equalsTo(String name, String method, List<Object> arg) {
        if (!Objects.equals(this.name, name) || !Objects.equals(this.method, method)) {
            return false;
        }
        if (this.arg == null || arg == null) {
            return this.arg == arg;
        }
        if (this.arg.size() != arg.size()) {
            return false;
        }
        for (int i = 0; i < this.arg.size(); i++) {
            if (!isEquals(this.arg.get(i), arg.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isEquals(Object o1, Object o2) throws NoSuchMethodException {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        if (o1.getClass() == o1.getClass().getMethod("equals", Object.class).getDeclaringClass()) {
            return Objects.equals(o1, o2);
        }
        return compCalculator.determineDifferences(o1, o2, compConfig).isEmpty();
    }
}
