package io.github.dimkich.integration.testing.assertion;

import java.io.File;
import java.util.Objects;

public class FileOperations {
    public void clearTestsDir() {
        File file = new File(AssertionConfig.resultDir);
        if (file.exists()) {
            purgeDirectory(file);
        }
    }

    private void purgeDirectory(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory())
                purgeDirectory(file);
            file.delete();
        }
    }
}
