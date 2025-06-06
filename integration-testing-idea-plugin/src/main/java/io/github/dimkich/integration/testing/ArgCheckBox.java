package io.github.dimkich.integration.testing;

import javax.swing.*;
import java.awt.*;

public class ArgCheckBox extends JPanel implements Arg {
    private final JCheckBox checkBox;
    private final String arg;

    public ArgCheckBox(String text, String arg, String tip) {
        super(new FlowLayout(FlowLayout.LEFT));
        checkBox = new JCheckBox(text);
        add(checkBox);
        this.arg = arg;
        setToolTipText("<html><p width=\"200\">" + tip + "</p></html>");
        checkBox.setToolTipText(getToolTipText());
    }

    public String getArg() {
        return checkBox.isSelected() ? arg : null;
    }

    @Override
    public String getText() {
        return checkBox.getText();
    }

    @Override
    public String getValue() {
        return Boolean.toString(checkBox.isSelected());
    }

    @Override
    public void setValue(String value) {
        checkBox.setSelected(Boolean.parseBoolean(value));
    }
}
