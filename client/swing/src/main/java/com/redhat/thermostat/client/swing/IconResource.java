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

import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.ui.IconDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to various icons.
 */
public class IconResource {
    
    private boolean fromFileSytem = false;
       
    public static final IconResource JAVA_APPLICATION_32 = new IconResource("java_application_identifier_32.png");
    public static final IconResource JAVA_APPLICATION_24 = new IconResource("java_application_identifier_24.png");
    public static final IconResource JAVA_APPLICATION = new IconResource("java_application_identifier.png");
    
    public static final IconResource HOST_32 = new IconResource("computer_32.png");
    public static final IconResource HOST_24 = new IconResource("computer_24.png");
    public static final IconResource HOST = new IconResource("computer.png");
    
    public static final IconResource SEARCH = new IconResource("search.png");
    public static final IconResource RECORD = new IconResource("record.png");
    public static final IconResource SAMPLE = new IconResource("sample.png");
    public static final IconResource CLEAN = new IconResource("clean.png");
    public static final IconResource TRASH = new IconResource("trash.png");
    
    public static final IconResource ARROW_LEFT = new IconResource("arrow-left.png");
    public static final IconResource ARROW_RIGHT = new IconResource("arrow-right.png");

    // TODO: add a proper one for this
    public static final IconResource HISTORY = RECORD;

    private final String path;

    private IconResource(String descriptor) {
        this.path = descriptor;
    }

    private IconResource(String descriptor, boolean fromFileSytem) {
        this.path = descriptor;
        this.fromFileSytem = fromFileSytem;
    }

    public Icon getIcon() {
        try {
            IconDescriptor descriptor = toIconDescriptor();
            return new Icon(descriptor);
            
        } catch (IOException ex) {
            Logger.getLogger(IconResource.class.getName()).log(Level.WARNING, "can't load icon: " + getPath(), ex);
        }
        return null;
    }

    String getPath() {
        return path;
    }
    
    public IconDescriptor toIconDescriptor() throws IOException {
        IconDescriptor icon = null;
        if (fromFileSytem) {
            icon = IconDescriptor.loadIcon(new File(getPath()));
        } else {
            icon = IconDescriptor.loadIcon(IconResource.class.getClassLoader(), getPath());
        }
        return icon;
    }
}

