package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
public abstract class TestCaseInit {
    public enum Apply {All, TestContainer, TestCase, TestPart}

    @JacksonXmlProperty(isAttribute = true)
    private Apply applyTo;
    @JsonBackReference
    private TestCase testCase;

    public boolean isApplicable(TestCase testCase) {
        if (applyTo == null) {
            return this.testCase == testCase;
        }
        if (applyTo == Apply.All) {
            return true;
        }
        return switch (testCase.getType()) {
            case TestContainer -> applyTo == Apply.TestContainer;
            case TestCase -> applyTo == Apply.TestCase;
            case TestPart -> applyTo == Apply.TestPart;
        };
    }

    public interface Initializer<T extends TestCaseInit> extends Comparable<Initializer<T>> {
        Class<T> getTestCaseInitClass();

        default Integer getOrder() {
            return Integer.MAX_VALUE;
        }

        @Override
        default int compareTo(Initializer<T> o) {
            return getOrder().compareTo(o.getOrder());
        }

        void init(Collection<T> inits) throws Exception;
    }
}
