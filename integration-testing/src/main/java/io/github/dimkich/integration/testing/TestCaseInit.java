package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;

@Data
public abstract class TestCaseInit implements Comparable<TestCaseInit> {
    @JacksonXmlProperty(isAttribute = true)
    private Integer level;
    @JsonBackReference
    private TestCase testCase;

    @JsonIgnore
    public int getActualLevel() {
        return testCase.getLevel() + (level == null ? 0 : level);
    }

    @JsonIgnore
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int compareTo(@Nonnull TestCaseInit o) {
        return getOrder().compareTo(o.getOrder());
    }

    public interface TestCaseInitializer<T extends TestCaseInit> {
        Class<T> getTestCaseInitClass();

        void init(T testCaseInit);
    }
}
