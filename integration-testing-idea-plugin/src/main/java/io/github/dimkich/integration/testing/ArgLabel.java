package io.github.dimkich.integration.testing;

import javax.swing.*;

public class ArgLabel extends JLabel implements Arg {
    public ArgLabel(String text) {
        super(text);
    }

    @Override
    public String getArg() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public void setValue(String value) {
    }
}
