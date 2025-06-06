package io.github.dimkich.integration.testing;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

final class IntegrationTestsFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        IntegrationTestsContent integrationTestsContent = new IntegrationTestsContent();
        Content content = ContentFactory.getInstance().createContent(integrationTestsContent, "", false);
        integrationTestsContent.init(project, content, toolWindow);
        toolWindow.getContentManager().addContent(content);
    }
}
