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

package com.redhat.thermostat.ui.swing.components;

import com.redhat.thermostat.animation.Animation;
import com.redhat.thermostat.animation.DefaultClip;
import com.redhat.thermostat.beans.property.DoubleProperty;
import com.redhat.thermostat.platform.swing.components.ThermostatComponent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;

/**
 */
public class SpinnerLayerUI extends LayerUI<ThermostatComponent> {

    private static final String THETA = "THETA";
    private DoubleProperty thetaProperty;

    private Animation spinner;

    public SpinnerLayerUI() {
        thetaProperty = new DoubleProperty(0, THETA);

        // animation fps, the number of ticks in the spinner and the
        // changes in theta during the animation have been chosen because
        // they seem to look good by empirical testing, but not by any
        // specific mathematical calculation, although doing the math will
        // probably show that this relationship is actually "correct" anyway
        spinner = new Animation(15);
        spinner.addClip(new Spinning());
    }

    public void start() {
        spinner.play();
    }

    public void stop() {
        spinner.stop();
    }

    @Override
    public void applyPropertyChange(PropertyChangeEvent evt, JLayer<? extends ThermostatComponent> l) {
        if (evt.getPropertyName().equals(THETA)) {
            thetaProperty.set((double) evt.getNewValue());
            l.repaint();
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        if (spinner.getStatus().equals(Animation.Status.STOPPED)) {
            return;
        }

        double totalWidth = c.getWidth();
        double totalHeight = c.getHeight();

        // we take only about a fourth of the total container real estate
        double width = totalWidth > totalHeight ? totalHeight/4 : totalWidth/4;

        // then again the longest side of a single tick is around 60% of the
        // space we allocated, so the actual real space used by the whole
        // spinner will be a bit more than just a fourth of the container
        // size
        width = width*60/100;
        double x = width*10/100;
        double height = width*15/100;

        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);


        AffineTransform original = graphics.getTransform();

        AffineTransform translate =
                AffineTransform.getTranslateInstance(totalWidth/2., totalHeight/2.);

        double theta = thetaProperty.get();
        for (int lines = 0; lines < 14; lines++) {
            AffineTransform rotate = AffineTransform.getRotateInstance(theta);
            theta += .45;
            graphics.setColor(new Color(0, 51, 153, 255/14*lines));
            graphics.transform(translate);
            graphics.transform(rotate);
            paintOneTick(graphics, x, width, height);
            graphics.setTransform(original);
        }

        graphics.dispose();
    }

    private void paintOneTick(Graphics2D graphics, double x, double width, double height) {

        RoundRectangle2D roundTick =
                new RoundRectangle2D.Double(x, - height/2, width, height, 15, 15);

        graphics.fill(roundTick);
    }

    class Spinning extends DefaultClip {

        @Override
        public void render(final double frame) {
            firePropertyChange(thetaProperty.getName(),
                               thetaProperty.get(),
                               thetaProperty.get() + 0.5);
        }
    }
}
