package com.redhat.thermostat.client;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Translate {

    private static ResourceBundle resourceBundle = null;

    static {
        resourceBundle = ResourceBundle.getBundle("com.redhat.thermostat.client.strings");
    }

    public static String _(String toTranslate) {
        return resourceBundle.getString(toTranslate);
    }

    public static String _(String toTranslate, String... params) {
        return MessageFormat.format(_(toTranslate), (Object[]) params);
    }
}
