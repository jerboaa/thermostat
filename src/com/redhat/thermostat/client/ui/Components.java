package com.redhat.thermostat.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class Components {
    public static JLabel header(String text) {
        JLabel label = new JLabel(HtmlTextBuilder.boldHtml(text));
        label.setHorizontalAlignment(SwingConstants.LEADING);
        return label;
    }

    public static JLabel label(String string) {
        JLabel label = new JLabel(string);
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        return label;
    }

    public static JLabel value(String value) {
        JLabel toDisplay = new JLabel(value);
        toDisplay.setHorizontalAlignment(SwingConstants.LEADING);
        return toDisplay;
    }

    public static Border smallBorder() {
        return BorderFactory.createEmptyBorder(5, 5, 5, 5);
    }
}
