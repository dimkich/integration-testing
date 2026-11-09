package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a single stubbed call configuration.
 * <p>
 * The instance holds a method name, arguments and a sequence of {@link StubCallResult}
 * objects that should be returned or thrown on subsequent invocations. Argument matching
 * is performed by {@link io.github.dimkich.integration.testing.comparison.ObjectComparisonService}.
 */
@Data
@Accessors(chain = true)
public class StubCall {
    /**
     * Stub name. Usually corresponds to a bean or component name.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    /**
     * Stubbed method name.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String method;
    /**
     * If {@code true}, argument comparison is skipped and only name/method are checked.
     */
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean disabled;
    /**
     * Expected arguments for the invocation; {@code null} is allowed.
     */
    private List<Object> arg;
    /**
     * Sequence of results/exceptions that will be produced by this stub.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<StubCallResult> result;
    @JsonIgnore
    private int resultIndex = 0;
    /**
     * Whether this call was used during the current test run.
     * <p>
     * A call is marked as used when it matches a stub invocation or when it is recorded
     * after a real method invocation. Unused calls are removed from leaf tests when the
     * test is updated.
     */
    @JsonIgnore
    private boolean used;

    /**
     * Appends a normal return value to the list of results.
     *
     * @param result value that should be returned on invocation
     */
    public void addResult(Object result) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new StubCallResult(result));
    }

    /**
     * Appends an exception to the list of results.
     *
     * @param e exception that should be thrown on invocation
     */
    public void addException(Throwable e) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new StubCallResult(e));
    }

    /**
     * Resets the internal result index so that subsequent invocations start
     * from the first configured {@link StubCallResult}.
     */
    @JsonIgnore
    public void reset() {
        resultIndex = 0;
    }

    /**
     * Returns the current configured result value for this invocation and
     * advances the internal index. If no results are configured, {@code null}
     * is returned.
     *
     * @return current result value or {@code null} if none
     */
    @JsonIgnore
    public Object getCurrentResult() {
        if (this.result == null) {
            return null;
        }
        StubCallResult r = result.get(resultIndex % result.size());
        resultIndex++;
        return r.getReturn1();
    }

    /**
     * If the current {@link StubCallResult} contains an exception, advances
     * the internal index and throws that exception.
     *
     * @throws Throwable when the current result is configured with an exception
     */
    @JsonIgnore
    public void tryThrowException() throws Throwable {
        if (this.result == null) {
            return;
        }
        StubCallResult r = result.get(resultIndex % result.size());
        if (r.getThrow1() != null) {
            resultIndex++;
            throw r.getThrow1();
        }
    }
}
