/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.platform.swing.components.ThermostatComponent;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.ui.swing.components.Spinner;
import com.redhat.thermostat.ui.swing.components.SpinnerLayerUI;

import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 */
public class SpinningPanel extends JPanel {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private SpinnerLayerUI spinnerLayerUI;

    private JLayer<ThermostatComponent> spinnerLayer;
    private boolean spinnerEnabled;

    private UIDefaults defaults;

    private ThermostatComponent content;

    private void hijack(MouseEvent e) {
        if (spinnerEnabled) {
            e.consume();
        }
    }

    boolean isSpinnerEnabled() {
        return spinnerEnabled;
    }

    class MouseEventHijector extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            hijack(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            hijack(e);
        }
    }

    public SpinningPanel(UIDefaults defaults) {
        this.defaults = defaults;

        spinnerEnabled = false;
        setOpaque(false);

        setLayout(new BorderLayout());

        spinnerLayerUI = new SpinnerLayerUI();
        ThermostatComponent spinner = new ThermostatComponent();
        spinnerLayer = new JLayer<>(spinner, spinnerLayerUI);

        MouseEventHijector hijector = new MouseEventHijector();

        setFocusTraversalKeysEnabled(false);

        content = new ThermostatComponent();
        content.addMouseListener(hijector);
        content.addMouseMotionListener(hijector);
        createHelpMessage();

        content.add(spinnerLayer, BorderLayout.CENTER);

        setVisible(false);
    }

    private void createHelpMessage() {

        ThermostatComponent component = new ThermostatComponent();
        component.setBorder(new EmptyBorder(0, 30, 30, 30));

        String wrappedText = "<html>" + translator.localize(LocaleResources.PROFILER_DESCRIPTION).getContents() + "</html>";
        JLabel descriptionLabel = new JLabel(wrappedText);

        component.add(descriptionLabel);

        content.add(component, BorderLayout.SOUTH);
    }

    void enableSpinner(boolean enable) {
        if (enable == spinnerEnabled) {
            return;
        }

        spinnerEnabled = enable;

        if (spinnerEnabled) {
            add(content);
            spinnerLayerUI.start();
        } else {
            spinnerLayerUI.stop();
            removeAll();
        }
        setVisible(enable);

        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (spinnerEnabled) {
            super.paintComponent(g);

            Graphics2D graphics = (Graphics2D) g.create();

            AlphaComposite composite = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.85f);
            graphics.setComposite(composite);

            graphics.setPaint(defaults.getComponentBGColor());
            graphics.fillRect(0, 0, getWidth(), getHeight());

            graphics.dispose();
        }
    }
}
