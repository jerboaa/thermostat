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

package com.redhat.thermostat.client.swing.internal.components;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ThermostatButton extends JButton {

    private float blend;

    private Timer blendTimer;

    private UIDefaults defaults;

    private Paint backgroundPaint;
    private Paint selectedBackgroundPaint;

    private Paint foregroundPaint;
    private Paint selectedForegroundPaint;

    public ThermostatButton(String text) {
        this(text, null);
    }

    public ThermostatButton(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    protected void init(String text, Icon icon) {
        super.init(text, icon);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);

        setOpaque(false);

        blend = 0.f;

        defaults = new UIDefaultsImpl();

        blendTimer = new Timer(3, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                blend += 0.016f;
                if (blend > 1) {
                    blend = 1.f;
                    blendTimer.stop();
                }
                repaint();
            }
        });
        blendTimer.setInitialDelay(0);
        blendTimer.setRepeats(true);

        setUI(new ThermostatButtonUI());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (blendTimer != null) {
                    blendTimer.start();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                blend = 0.f;
                if (blendTimer != null) {
                    blendTimer.stop();
                }
            }
        });
    }


    public Paint getBackgroundPaint() {
        if (backgroundPaint == null) {
            backgroundPaint = defaults.getComponentBGColor();
        }
        return backgroundPaint;
    }

    public void setBackgroundPaint(Paint backgroundPaint) {
        this.backgroundPaint = backgroundPaint;
        repaint();
    }

    public Paint getSelectedBackgroundPaint() {
        if (selectedBackgroundPaint == null) {
            selectedBackgroundPaint = defaults.getSelectedComponentBGColor();
        }
        return selectedBackgroundPaint;
    }

    public void setSelectedBackgroundPaint(Paint selectedBackgroundPaint) {
        this.selectedBackgroundPaint = selectedBackgroundPaint;
        repaint();
    }

    public Paint getSelectedForegroundPaint() {
        if (selectedForegroundPaint == null) {
            selectedForegroundPaint = defaults.getSelectedComponentFGColor();
        }
        return selectedForegroundPaint;
    }

    public void setSelectedForegroundPaint(Paint selectedForegroundPaint) {
        this.selectedForegroundPaint = selectedForegroundPaint;
        repaint();
    }

    public void setForegroundPaint(Paint foregroundPaint) {
        this.foregroundPaint = foregroundPaint;
        repaint();
    }

    public Paint getForegroundPaint() {
        if (foregroundPaint == null) {
            foregroundPaint = defaults.getComponentFGColor();
        }
        return foregroundPaint;
    }

    float getBlend() {
        return blend;
    }
}
