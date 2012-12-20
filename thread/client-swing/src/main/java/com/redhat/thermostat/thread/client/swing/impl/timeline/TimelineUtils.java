/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.LongRange;
import com.redhat.thermostat.common.model.LongRangeNormalizer;


public class TimelineUtils {
    public static final int INC = 50;
    public static final int STEP = 1000;
    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);

    public static int calculateWidth(LongRange range) {
        long span = range.getMax() - range.getMin();
        int width = (int) (span / TimelineUtils.INC);
        return width;
    }
    
    public static int drawMarks(LongRange range, Graphics2D graphics, Rectangle bounds, int currentValue, int width, int height) {
        return drawMarks(range, graphics, bounds, currentValue, width, height, false);
    }
    
    public static int drawMarks(LongRange range, Graphics2D graphics, Rectangle bounds, int currentValue, int width, int height, boolean darkerTop) {
        LongRangeNormalizer normalizer = new LongRangeNormalizer(range, 0, width);
        normalizer.setValue(range.getMin() + TimelineUtils.STEP);
        int totalInc = (int) normalizer.getValueNormalized();

        int inc = currentValue % totalInc;
        int x = (bounds.x - inc);
        
        graphics.setColor(Palette.GRAY.getColor());
        int upperBound = (bounds.x + bounds.width);

        for (int i = x; i < upperBound; i += totalInc) {
            graphics.drawLine(i, 0, i, height);
            if (darkerTop) {
                graphics.setColor(Palette.DARK_GRAY.getColor());
                graphics.drawLine(i, 0, i, 5);
                graphics.setColor(Palette.GRAY.getColor());
            }
        }
        
        return totalInc;
    }
}
