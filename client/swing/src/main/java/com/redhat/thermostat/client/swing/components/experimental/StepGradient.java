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

package com.redhat.thermostat.client.swing.components.experimental;

import com.redhat.thermostat.common.model.Interpolator;

import java.awt.Color;

/**
 */
public class StepGradient {

    private Color start;
    private Color end;

    private int steps;

    private float[] startHue;
    private float[] endHue;

    private double angle;
    private double direction;

    private float currentStep;

    public StepGradient(Color start, Color end, int steps) {
        this.start = start;
        this.end = end;
        this.steps = steps;

        startHue = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
        endHue = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

        angle = 1./steps;
        direction = angle;

        currentStep = 0.f;
    }

    public Color getEndColor() {
        return end;
    }

    public Color getStartColor() {
        return start;
    }
    public void reset() {
        currentStep = 0.f;
        direction = angle;
    }

    public Color sample() {

        float[] color = new float[3];

        color[0] = Interpolator.lerp(startHue[0], endHue[0], currentStep);
        color[1] = Interpolator.lerp(startHue[1], endHue[1], currentStep);
        color[2] = Interpolator.lerp(startHue[2], endHue[2], currentStep);

        currentStep += direction;
        if (currentStep >= 1) {
            direction = -angle;
        } else if (currentStep <= 0) {
            direction = angle;
        }

        return new Color(Color.HSBtoRGB(color[0], color[1], color[2]));
    }
}
