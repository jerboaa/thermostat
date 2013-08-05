/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal.progress;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import com.redhat.thermostat.client.swing.components.AbstractLayout;
import com.redhat.thermostat.client.swing.components.OverlayPanel;

public class AggregateProgressBarOverlayLayout extends AbstractLayout {

    @Override
    protected void doLayout(Container parent) {
        Component[] children = parent.getComponents();
        for (Component _child : children) {
            if (!(_child instanceof OverlayPanel)) {
                continue;
            }

            OverlayPanel child = (OverlayPanel) _child;
            if (!child.isVisible()) {
                continue;
            }
            
            // limit the size to some reasonable default so that
            // the panel doesn't go offscreen if it grows too much
            Dimension preferredSize = child.getPreferredSize();
            if (preferredSize.height > 300) {
                preferredSize.height = 300;
            }
            
            // FIXME: the magic number is referred to StatusBar grip icon
            // size. There's no way we can access it at the moment, so we
            // just rely on the fact that we know it's a 16x16 icon...
            // We should probably set this information somewhere so that
            // the layout will still work in the unlikely case this icon should
            // ever change
            int x = parent.getWidth() - preferredSize.width +
                    child.getInsets().left - 16 + 2;
            int y = parent.getHeight() - preferredSize.height + 16;
            Rectangle bounds =
                    new Rectangle(x, y, preferredSize.width, preferredSize.height);

            child.setSize(preferredSize);
            child.setBounds(bounds);
        }
    }
}
