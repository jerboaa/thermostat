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

package com.redhat.thermostat.swing;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalButtonUI;

class ActionButtonUI extends MetalButtonUI {

    private BufferedImage sourceIcon;
    private BufferedImage rollOverIcon;
    private AbstractButton realButton;
    
    ActionButtonUI(AbstractButton button) {
        this.realButton = button;
    }
    
    private BufferedImage getBrighterImage(BufferedImage source) {
        float[] kernel = new float[] { 0, 0, 0, 0, 1.2f, 0, 0, 0, 0 };

        BufferedImageOp brighterOp = new ConvolveOp(new Kernel(3, 3, kernel),
                ConvolveOp.EDGE_NO_OP, null);
        return brighterOp.filter(source, new BufferedImage(source.getWidth(),
                source.getHeight(), source.getType()));
    }

    @Override
    public void paint(Graphics g, JComponent c) {

        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        if (model.isPressed() || model.isArmed() || model.isSelected()) {
            realButton.paint(g);
        } else if (model.isRollover()) {
            Border border = realButton.getBorder();
            border.paintBorder(realButton, g, 0, 0, realButton.getWidth(),
                    realButton.getHeight());
        }
        // paint the icon, always to the center
        Icon icon = button.getIcon();
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        if (sourceIcon == null) {
            sourceIcon = new BufferedImage(w + 1, h + 1,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics imageGraphics = sourceIcon.getGraphics();
            icon.paintIcon(null, imageGraphics, 0, 0);
        }

        if (rollOverIcon == null) {
            rollOverIcon = getBrighterImage(sourceIcon);
        }

        int x = realButton.getWidth() / 2 - w / 2;
        int y = realButton.getHeight() / 2 - h / 2;

        if (model.isRollover()) {
            g.drawImage(rollOverIcon, x, y, null);
        } else {
            g.drawImage(sourceIcon, x, y, null);
        }
    }
}
