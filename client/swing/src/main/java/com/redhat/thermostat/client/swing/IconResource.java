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

package com.redhat.thermostat.client.swing;

import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconResource {
    /* FIXME we need to pick up the icons dynamically */

    private static final String ICON_PREFIX = "/usr/share/icons/gnome/";

    // an icon that should always be available and indicate that the actual icon
    // is missing.
    public static final IconResource MISSING_ICON = null;

    public static final IconResource JAVA_APPLICATION = new IconResource("duke.png");
    public static final IconResource HOST = new IconResource(ICON_PREFIX + "16x16/devices/computer.png");
    
    public static final IconResource ERROR = new IconResource(ICON_PREFIX + "48x48/status/dialog-error.png");
    public static final IconResource QUESTION = new IconResource(ICON_PREFIX + "48x48/status/dialog-question.png");
    public static final IconResource WARNING = new IconResource(ICON_PREFIX + "48x48/status/dialog-warning.png");

    public static final IconResource COMPUTER = new IconResource(ICON_PREFIX + "48x48/devices/computer.png");
    public static final IconResource NETWORK_SERVER = new IconResource(ICON_PREFIX + "48x48/places/network-server.png");
    public static final IconResource NETWORK_GROUP = new IconResource(ICON_PREFIX + "48x48/places/network-workgroup.png");

    public static final IconResource ARROW_RIGHT = new IconResource(ICON_PREFIX + "48x48/actions/go-next.png");

    public static final IconResource SEARCH = new IconResource(ICON_PREFIX + "16x16/actions/search.png");

    private final String path;

    private IconResource(String descriptor) {
        this.path = descriptor;
    }

    public static IconResource fromPath(String path) {
        // TODO implement this
        return null;
    }

    public Icon getIcon() {
        if (new File(path).exists()) {
            return new ImageIcon(path);
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return "file:" + getPath();
    }
}
