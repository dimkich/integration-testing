package io.github.dimkich.integration.testing;

/**
 * Represents a test part in the integration testing framework hierarchy.
 * <p>
 * A {@code TestPart} is a concrete implementation of {@link Test} that represents
 * an individual test part. It must be a child of a {@link TestCase} and serves as
 * the leaf level in the test hierarchy.
 * <p>
 * This class overrides {@link Test#getType()} to return {@link Test.Type#TestPart},
 * which identifies this test instance as a test part in the hierarchy.
 *
 * @author dimkich
 * @see Test
 * @see TestCase
 * @see TestContainer
 */
public class TestPart extends Test {
    @Override
    public Type getType() {
        return Type.TestPart;
    }
}
