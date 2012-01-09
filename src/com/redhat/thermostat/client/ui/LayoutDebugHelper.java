package com.redhat.thermostat.client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

/**
 * This class goes through the swing container hierarchy and adds random colored
 * borders to the components themselves. it makes it easier to see what the
 * {@code LayoutManager}s are doing
 */
public class LayoutDebugHelper {

    private Color[] colors = new Color[] { Color.BLACK, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.PINK, Color.ORANGE, Color.RED, Color.YELLOW };
    private int colorIndex = 0;

    public void debugLayout(Window w) {
        Component[] children = w.getComponents();
        debugLayout(children);
    }

    public void debugLayout(Component c) {
        if (c instanceof JComponent) {
            JComponent panel = (JComponent) c;
            try {
                panel.setBorder(BorderFactory.createLineBorder(colors[colorIndex % colors.length]));
            } catch (IllegalArgumentException iae) {
                // never mind then
            }
            colorIndex++;
            debugLayout(panel.getComponents());
        }
    }

    public void debugLayout(Component[] components) {
        for (Component aComponent : components) {
            debugLayout(aComponent);
        }
    }
}
