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
    @JacksonXmlProperty(isAttribute = true)
    private Integer level;
    @JsonBackReference
    private TestCase testCase;

    @JsonIgnore
    public int getActualLevel() {
        return testCase.getLevel() + (level == null ? 0 : level);
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
