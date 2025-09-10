package io.github.dimkich.integration.testing;

import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;

public class ArgComboBox extends JPanel implements Arg {
    private final JComboBox<String> comboBox;
    private final JLabel label;
    private final String arg;

    public ArgComboBox(String text, String arg, String tip, String... values) {
        super(new FlowLayout(FlowLayout.LEFT));
        label = new JLabel(text);
        add(label);
        comboBox = new ComboBox<>(values);
        add(comboBox);
        this.arg = arg;
        setToolTipText("<html><p width=\"200\">" + tip + "</p></html>");
        comboBox.setToolTipText(getToolTipText());
        label.setToolTipText(getToolTipText());
    }

    public String getArg() {
        return arg + comboBox.getSelectedItem();
    }

    @Override
    public String getText() {
        return label.getText();
    }

    @Override
    public String getValue() {
        return (String) comboBox.getSelectedItem();
    }

    @Override
    public void setValue(String value) {
        comboBox.setSelectedItem(value);
    }
}
