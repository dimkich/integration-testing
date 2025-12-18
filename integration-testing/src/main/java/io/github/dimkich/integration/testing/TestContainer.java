package io.github.dimkich.integration.testing;

/**
 * Represents a test container in the integration testing framework hierarchy.
 * <p>
 * A {@code TestContainer} is a concrete implementation of {@link Test} that represents
 * a container for grouping test cases or other test containers. It can serve as the root
 * of the test hierarchy or be nested within another {@code TestContainer}.
 * <p>
 * The root test must always be of type {@code TestContainer}. Nested containers allow
 * for hierarchical organization of test cases, providing logical grouping and structure
 * to the test suite.
 * <p>
 * This class overrides {@link Test#getType()} to return {@link Test.Type#TestContainer},
 * which identifies this test instance as a test container in the hierarchy.
 *
 * @author dimkich
 * @see Test
 * @see TestCase
 * @see TestPart
 */
public class TestContainer extends Test {
    @Override
    public Type getType() {
        return Type.TestContainer;
    }
}
