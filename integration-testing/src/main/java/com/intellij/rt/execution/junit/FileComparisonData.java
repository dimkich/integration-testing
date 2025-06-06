package com.intellij.rt.execution.junit;

/**
 * Intellij Idea marker for showing file comparison dialog on assertion failure, instead of string comparison
 */
public interface FileComparisonData {
    String getActualStringPresentation();

    String getExpectedStringPresentation();

    String getFilePath();

    String getActualFilePath();
}
