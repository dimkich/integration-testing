package io.github.dimkich.integration.testing;

/**
 * Represents a test case in the integration testing framework hierarchy.
 * <p>
 * A {@code TestCase} is a concrete implementation of {@link Test} that represents
 * an individual test case. It must be a child of a {@link TestContainer} and may
 * contain {@link TestPart} instances as its children, or may not contain any children.
 * <p>
 * This class overrides {@link Test#getType()} to return {@link Test.Type#TestCase},
 * which identifies this test instance as a test case in the hierarchy.
 *
 * @author dimkich
 * @see Test
 * @see TestContainer
 * @see TestPart
 */
public class TestCase extends Test {
    @Override
    public Type getType() {
        return Type.TestCase;
    }
}
