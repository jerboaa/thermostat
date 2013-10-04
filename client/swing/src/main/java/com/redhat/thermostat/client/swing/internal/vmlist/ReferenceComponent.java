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

package com.redhat.thermostat.client.swing.internal.vmlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.CompositeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.storage.core.Ref;

@SuppressWarnings("serial")
public class ReferenceComponent extends JPanel implements AccordionComponent, ReferenceProvider {

    private ShadowLabel mainLabel;
    private JLabel iconLabel;
    
    private boolean highlight;
    
    private boolean selected;
    
    private Icon selectedIcon;
    private Icon icon;
    
    private ReferenceComponentPainter painter;
    
    private Ref vm;
    
    public ReferenceComponent(Ref vm) {
        
        this.vm = vm;
        this.painter = new ReferenceComponentPainter();
        
        setLayout(new BorderLayout());

        mainLabel = new ShadowLabel();
        mainLabel.setForeground(Palette.DROID_GRAY.getColor());

        mainLabel.setText(vm.getName());
        add(mainLabel, BorderLayout.CENTER);
        iconLabel = new JLabel();
        iconLabel.setText(" ");
        add(iconLabel, BorderLayout.WEST);
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        setState();
    }

    private void setState() {
        UIDefaults palette = UIDefaults.getInstance();
        if (selected) {
            mainLabel.setForeground(palette.getSelectedComponentFGColor());
            mainLabel.setIcon(selectedIcon);
        } else if (!highlight) {
            mainLabel.setForeground(palette.getComponentFGColor());
            mainLabel.setIcon(icon);
        }
        repaint();        
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public Ref getReference() {
        return vm;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        painter.paint((Graphics2D) g, this, getWidth(), getHeight());
    }
    
    public void setIcon(Icon icon) {
        this.icon = icon;
        this.selectedIcon = new CompositeIcon(icon, new BaseIcon(true, icon));
        setState();
    }

    public Icon getIcon() {
        return icon;
    }
}
