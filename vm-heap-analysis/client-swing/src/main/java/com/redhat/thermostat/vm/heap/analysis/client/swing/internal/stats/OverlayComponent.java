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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.CompositeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.timeline.TimelineUtils;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapIconResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.Transient;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("serial")
public class OverlayComponent extends ShadowLabel {

    private HeapDump dump;
    
    private boolean selected;
    private String _text;
    
    public class NiceIcon extends Icon {
        
        private Icon src;
        
        public NiceIcon(Icon src) {
            this.src = src;
        }
        
        @Override
        public int getIconHeight() {
            return src.getIconHeight();
        }
        
        @Override
        public int getIconWidth() {
            return src.getIconWidth();
        }
        
        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {

            GraphicsUtils utils = GraphicsUtils.getInstance();
            Graphics2D graphics = utils.createAAGraphics(g);

            graphics.setColor(utils.deriveWithAlpha(OverlayComponent.this.getForeground(), 200));
            
            graphics.fillRect(x, y, getIconWidth(), getIconHeight());
            
            graphics.dispose();
        }
        
        @Override
        @Transient
        public Image getImage() {
            BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) image.getGraphics();
            paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }
    
    public OverlayComponent(HeapDump dump) {
        
        super(LocalizedString.EMPTY_STRING);
        
        setOpaque(false);
        
        setForeground(Palette.ROYAL_BLUE.getColor());
        
        setName(OverlayComponent.class.getName());
        
        Icon mask = new Icon(HeapIconResources.getIcon(HeapIconResources.PIN_MASK));
        Icon source = new NiceIcon(mask);
        
        setIcon(new CompositeIcon(mask, source));
        
        this.dump = dump;

        this._text = new Date(dump.getTimestamp()).toString();
        
        setFont(TimelineUtils.FONT);

        setOpaque(false);

        setToolTipText(_text);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected()) {
                    setText(_text);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected()) {
                    setText("");
                    repaint();
                }
            }
        });
    }
    
    public HeapDump getHeapDump() {
        return this.dump;
    }
    
    public long getTimestamp() {
        return dump.getTimestamp();
    }

    @Transient
    public Dimension getIconCenter() {
        Dimension preferredSize = ((Icon) getIcon()).getDimension();

        preferredSize.width = (preferredSize.width / 2);
        preferredSize.height = (preferredSize.height/2);

        return preferredSize;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(Long.valueOf(dump.getTimestamp()));
    }

    @Override
    public boolean equals(Object obj) {        
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OverlayComponent other = (OverlayComponent) obj;
        if (getTimestamp() != other.getTimestamp())
            return false;
        return true;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            setText(_text);
        } else {
            setText("");
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
