/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.ui.Decorator;
import com.redhat.thermostat.client.ui.PlatformIcon;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.storage.core.Ref;

/**
 * This {@link Enum} marks {@link Decorator} target for
 * {@link ReferenceFieldLabelDecorator}s and
 * {@link ReferenceFieldIconDecorator}s. Each of those entry point represent
 * a visual element in the reference list component for the Swing based client.
 */
public enum ReferenceFieldDecoratorLayout {
    
    /**
     * Represents the main label portion to be decorated by
     * {@link ReferenceFieldLabelDecorator}s.
     * 
     * <br /><br />
     * 
     * The main label is the top most field for each entry in the
     * {@link Ref}eference list, usually containing by default the name of
     * the reference.
     * 
     * <br /><br />
     * 
     * An example use for this field is the name of the target reference.
     */
    LABEL_MAIN,
    
    /**
     * Represents the secondary label portion to be decorated by
     * {@link ReferenceFieldLabelDecorator}.
     * 
     * <br /><br />
     * 
     * This label is usually a smaller and less obvious visual component that
     * helps better define the actual entry. It should not contain too much
     * information to avoid cluttering the UI.
     * 
     * <br /><br />
     * 
     * An example use of this field is an unique ID of some kind.
     */
    LABEL_INFO,
    
    /**
     * Represents the main {@link PlatformIcon} portion of the entry to be
     * decorated by {@link ReferenceFieldIconDecorator}s.
     * 
     * <br /><br />
     * 
     * This icon is the main icon characterising the component being shown,
     * for example representing a particular application or device.
     *
     * <br /><br />
     * 
     * Decorators registered for this entry point can cast PlatformIcon into
     * {@link Icon}.
     * 
     * <br /><br />
     * 
     * <strong>Note</strong>: since this client doesn't provide a default icon,
     * plugins that apply for this decoration should check for a
     * <code>null</code> PlatformIcon before using the input parameter. 
     */
    ICON_MAIN,
}

