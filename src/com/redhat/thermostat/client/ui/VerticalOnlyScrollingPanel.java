package com.redhat.thermostat.client.ui;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * A JPanel, that when added to a JScrollPane shows allows vertical scrolling.
 */
public class VerticalOnlyScrollingPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = -1039658895594239585L;

    public VerticalOnlyScrollingPanel() {
        super();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // FIXME
        return 5;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // FIXME
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) getParent();
            return getPreferredSize().getHeight() < viewport.getHeight();
        }
        return false;
    }

}
