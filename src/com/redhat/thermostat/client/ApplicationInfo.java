package com.redhat.thermostat.client;

import java.util.Date;

import javax.swing.Icon;

import com.redhat.thermostat.client.ui.IconResource;

public class ApplicationInfo {

    /*
     * TODO All of these fields should be generated on build.
     */

    public String getName() {
        return "thermostat";
    }

    public String getVersion() {
        return "0.0.1";
    }

    public String getDescription() {
        return "A monitoring and servicability tool for OpenJDK";
    }

    public Icon getIcon() {
        return IconResource.QUESTION.getIcon();
    }

    public String getReleaseDate() {
        return "2012-02-02";
    }

    public String getBuildDate() {
        return new Date().toString();
    }

    public String getCopyright() {
        return "(C) Copyright Red Hat, Inc";
    }

    public String getLicense() {
        return "GPL2 + Classpath";
    }

    public String getEmail() {
        return "an@example.com";
    }

    public String getWebsite() {
        return "http://an.example.com";
    }

}
