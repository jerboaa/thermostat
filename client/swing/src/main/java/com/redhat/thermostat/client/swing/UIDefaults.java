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

package com.redhat.thermostat.client.swing;

import java.awt.Color;
import java.awt.Paint;

import com.redhat.thermostat.annotations.Service;

/**
 * Returns the default UI defaults used by this Thermostat client.
 */
@Service
public interface UIDefaults {
    
    /**
     * Returns the foreground colour for components that are
     * selected, like text or entries in the reference field panel.
     */
    Paint getSelectedComponentFGColor();

    /**
     * Returns the foreground colour for components.
     */
    Paint getComponentFGColor();

    /**
     * Returns the suggested background colour for components that are
     * selected.
     */
    Paint getSelectedComponentBGColor();

    /**
     * Returns the default background colour for UI components.
     */
    Paint getComponentBGColor();
    
    /**
     * Returns the suggested foreground colour for components that want to
     * differentiate their text from other components. This is used, for example,
     * in the side panel for {@link ReferenceFieldDecoratorLayout#LABEL_INFO}
     * components.
     */
    Paint getComponentSecondaryFGColor();
    
    /**
     * Returns the suggested size for the
     * {@link ReferenceFieldDecoratorLayout#ICON_MAIN} icons.
     * 
     * <br /><br />
     * 
     * <strong>Note</strong>: by default the side panel is not annotated
     * with icons, this is a suggested value for plugins that want to provide
     * an ReferenceFieldDecoratorLayout main icon decoration and
     * indicates the full space of the icon (not the actual drawn area).
     */
    int getReferenceFieldDefaultIconSize();
    
    /**
     * Returns the suggested size for the icon decorations, where applicable.
     */
    int getIconDecorationSize();
    
    /**
     * Returns the suggested size for icons to be used in Thermostat views and
     * components.
     */
    int getIconSize();
    
    /**
     * Returns the suggested color for icons to be used in Thermostat views and
     * components.
     */
    Color getIconColor();
    
    /**
     * Returns the suggested color for icons to be used as
     * {@link ReferenceFieldDecoratorLayout#ICON_MAIN} entry point.
     */
    Paint getReferenceFieldIconColor();
    
    /**
     * Returns the suggested color for icons to be used as
     * {@link ReferenceFieldDecoratorLayout#ICON_MAIN} entry point when
     * selected.
     */
    Paint getReferenceFieldIconSelectedColor();
    
    /**
     * Returns the suggested color for icons decorations to be used in
     * Thermostat views and components.
     */
    Paint getDecorationIconColor();
}

