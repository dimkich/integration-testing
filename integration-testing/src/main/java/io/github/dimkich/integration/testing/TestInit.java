package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Stream;

@Getter
@Setter
public abstract class TestInit {
    public enum Apply {All, TestContainer, TestCase, TestPart}

    @JacksonXmlProperty(isAttribute = true)
    private Apply applyTo;
    @JsonBackReference
    private Test test;

    public interface Initializer<T extends TestInit> extends Comparable<Initializer<T>> {
        Class<T> getTestInitClass();

        default Integer getOrder() {
            return Integer.MAX_VALUE;
        }

        @Override
        default int compareTo(Initializer<T> o) {
            return getOrder().compareTo(o.getOrder());
        }

        void init(Stream<T> inits) throws Exception;
    }
}
