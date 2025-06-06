package io.github.dimkich.integration.testing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;

import javax.swing.*;
import java.awt.*;

public class IntegrationTestsContent extends JPanel {
    private final FileAssertSaver fileAssertSaver = new FileAssertSaver();
    private static final Logger log = Logger.getInstance(IntegrationTestsContent.class);

    public void init(Project project, Content content, ToolWindow toolWindow) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (Arg agr : ArgStorage.getInstance().getAll()) {
            add((Component) agr, gbc);
            gbc.gridy++;
        }

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        JButton newLineButton = new JButton("Save file assertion");
        newLineButton.addActionListener(l -> {
            try {
                fileAssertSaver.save(project);
            } catch (Exception e) {
                Messages.showErrorDialog(project, e.getClass().getName() + " " + e.getMessage(), "Error Saving Assertion File");
            }
        });
        add(newLineButton, gbc);
    }
}
