package io.github.dimkich.integration.testing.assertion;

import com.intellij.rt.execution.junit.FileComparisonData;
import junit.framework.ComparisonFailure;
import lombok.Getter;

import java.io.File;

@Getter
public class FileComparisonFailure extends ComparisonFailure implements FileComparisonData {
    private final String filePath;
    private final String actualFilePath;
    private final String actualStringPresentation;
    private final String expectedStringPresentation;

    public FileComparisonFailure(String message, String expected, String actual, String expectedFilePath,
                                 String actualFilePath) {
        super(message, expected, actual);
        if (expected == null) throw new NullPointerException("'expected' must not be null");
        if (actual == null) throw new NullPointerException("'actual' must not be null");
        expectedStringPresentation = expected;
        this.actualStringPresentation = actual;
        filePath = expectedFilePath;
        if (expectedFilePath != null && !new File(expectedFilePath).isFile()) {
            throw new NullPointerException("'expectedFilePath' should point to the existing file or be null; got: "
                                           + expectedFilePath);
        }
        this.actualFilePath = actualFilePath;
    }
}
