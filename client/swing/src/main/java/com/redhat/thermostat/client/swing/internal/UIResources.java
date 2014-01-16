/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import com.redhat.thermostat.client.ui.IconDescriptor;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/**
 * Encapsulates a standard selection of colors and fonts.
 */
class UIResources {

    private static final UIResources resource = new UIResources();
    
    private static final ColorUIResource hyperLinkColor;
    private static final ColorUIResource hyperLinkActiveColor;

    private static final ColorUIResource selectionColor;
    
    private static final Font standard;
    
    private static IconDescriptor logo = null;
    
    static {
        Color color = UIManager.getColor("Button.darkShadow");
        if (color == null) {
            color = Color.BLUE;
        }
        hyperLinkColor = new ColorUIResource(color);

        color = UIManager.getColor("Button.focus");
        if (color == null) {
            color = Color.BLUE;
        }
        hyperLinkActiveColor = new ColorUIResource(color);
        selectionColor = hyperLinkActiveColor;
        
        Font font = UIManager.getFont("Label.font");
        if (font == null) {
            font = Font.decode(Font.DIALOG);
        }
        standard = font;
        try {
            logo = IconDescriptor.loadIcon(UIResources.class.getClassLoader(), "/icons/thermostat.png");
        } catch (IOException ex) {
            Logger.getLogger(UIResources.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    private static final Font header = standard.deriveFont(Font.BOLD);
    
    // TODO: check when size is too small
    private static final Font footer = standard.deriveFont(Font.PLAIN, standard.getSize() - 2);
    
    private UIResources() { /* nothing to do */ }
    
    // colors

    public static UIResources getInstance() {
        return resource;
    }
    
    public ColorUIResource hyperlinkColor() {
        return hyperLinkColor;
    }
    
    public ColorUIResource hyperlinkActiveColor() {
        return hyperLinkActiveColor;
    }
    
    public ColorUIResource getSelectionColor() {
        return selectionColor;
    }
    
    // font resources
    
    public Font footerFont() {
        return footer;
    }
    
    public Font headerFont() {
        return header;
    }
    
    public Font standardFont() {
        return standard;
    }
    
    // miscellaneous
    
    public IconDescriptor getLogo() {
        return logo;
    }
}

