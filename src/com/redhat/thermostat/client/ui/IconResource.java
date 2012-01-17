package com.redhat.thermostat.client.ui;

import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconResource {
    /* FIXME we need to pick up the icons dynamically */

    private static final String ICON_PREFIX = "/usr/share/icons/gnome/";

    // an icon that should always be available and indicate that the actual icon
    // is missing.
    public static final IconResource MISSING_ICON = null;

    public static final IconResource ERROR = new IconResource(ICON_PREFIX + "48x48/status/dialog-error.png");
    public static final IconResource QUESTION = new IconResource(ICON_PREFIX + "256x256/status/dialog-question.png");
    public static final IconResource WARNING = new IconResource(ICON_PREFIX + "48x48/status/dialog-warning.png");

    public static final IconResource COMPUTER = new IconResource(ICON_PREFIX + "48x48/devices/computer.png");
    public static final IconResource NETWORK_SERVER = new IconResource(ICON_PREFIX + "48x48/places/network-server.png");
    public static final IconResource NETWORK_GROUP = new IconResource(ICON_PREFIX + "48x48/places/network-workgroup.png");

    public static final IconResource SEARCH = new IconResource(ICON_PREFIX + "16x16/actions/search.png");

    public static final IconResource USER_HOME = new IconResource(ICON_PREFIX + "256x256/places/user-home.png");

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
