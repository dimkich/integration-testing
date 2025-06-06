package io.github.dimkich.integration.testing.assertion;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;

@Configuration
@Import({FileAssertion.class, SaveActualDataAssertion.class, SingleFileAssertion.class, StringAssertion.class,
        FileOperations.class})
public class AssertionConfig {
    public static final String ASSERTION_PROPERTY = "integration.testing.assertion";
    public static final String resultDir = System.getProperty("java.io.tmpdir") + "java_tests" + File.separator;
}
