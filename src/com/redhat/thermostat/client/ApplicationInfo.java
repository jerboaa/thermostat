package com.redhat.thermostat.client;

import static com.redhat.thermostat.client.Translate._;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import com.redhat.thermostat.client.ui.IconResource;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ApplicationInfo {

    private static final Logger logger = LoggingUtils.getLogger(ApplicationInfo.class);

    private static final String APP_INFO_ROOT = "com/redhat/thermostat/";

    private Properties appInfo;

    public ApplicationInfo() {
        appInfo = new Properties();
        ClassLoader cl = this.getClass().getClassLoader();
        InputStream res = cl.getResourceAsStream(APP_INFO_ROOT + "app-info.properties");
        if (res != null) {
            try {
                appInfo.load(res);
            } catch (IOException e) {
                logger.log(Level.WARNING, "error loading application information", e);
            }
        }
    }

    public String getName() {
        return appInfo.getProperty("APP_NAME", _("MISSING_INFO"));
    }

    public String getVersion() {
        return appInfo.getProperty("APP_VERSION", _("MISSING_INFO"));
    }

    public String getDescription() {
        return appInfo.getProperty("APP_DESCRIPTION", _("MISSING_INFO"));
    }

    public Icon getIcon() {
        String path = appInfo.getProperty("APP_ICON");
        if (new File(path).exists()) {
            return IconResource.fromPath(path).getIcon();
        }
        return IconResource.QUESTION.getIcon();
    }

    public String getReleaseDate() {
        return appInfo.getProperty("APP_RELEASE_DATE", _("MISSING_INFO"));
    }

    public String getCopyright() {
        return appInfo.getProperty("APP_COPYRIGHT", _("MISSING_INFO"));
    }

    public String getLicenseSummary() {
        return appInfo.getProperty("APP_LICENSE_SUMMARY", _("MISSING_INFO"));
    }

    public String getEmail() {
        return appInfo.getProperty("APP_EMAIL", _("MISSING_INFO"));
    }

    public String getWebsite() {
        return appInfo.getProperty("APP_WEBSITE", _("MISSING_INFO"));
    }

}
