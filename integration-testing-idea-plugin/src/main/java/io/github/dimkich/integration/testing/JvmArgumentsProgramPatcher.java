package io.github.dimkich.integration.testing;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;

import java.util.Set;

public class JvmArgumentsProgramPatcher extends JavaProgramPatcher {
    private static final Set<String> TEST_TYPES = Set.of("JUnit", "TestNG");

    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (configuration instanceof RunConfiguration runConfiguration) {
            String runType = runConfiguration.getType().getId();
            if (TEST_TYPES.contains(runType)) {
                for (String jvmArg : ArgStorage.getInstance().getJvmArgs()) {
                    javaParameters.getVMParametersList().addParametersString(jvmArg);
                }
            }
        }
    }
}
