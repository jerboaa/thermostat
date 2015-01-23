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

package com.redhat.thermostat.testutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Random;

// FIXME the methods in this class can probably be split more sanely
public class TestUtils {

	private static File sysAgentConf, userAgentConf, agentAuth;

    /**
     * @return the process id of the current process
     */
    public static int getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pidPart = name.split("@")[0];
        return Integer.parseInt(pidPart);
    }

    /**
     * @return true if the current os is linux
     */
    public static boolean isLinux() {
        return (System.getProperty("os.name").toLowerCase().contains("linux"));
    }

    /**
     * Creates and initializes a directory suitable for use as the storage's
     * configuration directory
     */
    public static String setupStorageConfigs(Properties dbConfig) throws IOException {
        Random random = new Random();

        String tmpDir = System.getProperty("java.io.tmpdir") + File.separatorChar +
                 Math.abs(random.nextInt()) + File.separatorChar;

        setupSystemStorageConfig(tmpDir, dbConfig);
        setupUserStorageConfig(tmpDir);

        return tmpDir;
    }

    private static void setupSystemStorageConfig(String root, Properties dbConfig) throws FileNotFoundException, IOException {
        System.setProperty("THERMOSTAT_HOME", root);

        File config = new File(root, "etc");
        config.mkdirs();
        File tmpConfigs = new File(config, "db.properties");

        dbConfig.store(new FileOutputStream(tmpConfigs), "thermostat test properties");
    }

    private static void setupUserStorageConfig(String root) {
        System.setProperty("USER_THERMOSTAT_HOME", root);

        new File(root, "run").mkdirs();
        new File(root, "logs").mkdirs();

        File data = new File(root + "data");
        data.mkdirs();
        new File(data, "db").mkdirs();
    }

    /**
     * Creates and initializes a directory suitable for use as the agent's
     * configuration directory
     * 
     * @deprecated use {@link #setupAgentConfigs(Properties, Properties)} instead
     */
    @Deprecated
    public static String setupAgentConfigs(Properties agentProperties) throws IOException {
        return setupAgentConfigs(agentProperties, agentProperties);
    }
    
    /**
     * Creates and initializes a directory suitable for use as the agent's
     * configuration directory
     */
    public static String setupAgentConfigs(Properties sysProps, Properties userProps) throws IOException {
        // need to create dummy config files for the tests
        Random random = new Random();

        String tmpDir = System.getProperty("java.io.tmpdir") + File.separatorChar +
                Math.abs(random.nextInt()) + File.separatorChar;

        System.setProperty("THERMOSTAT_HOME", tmpDir);
        
        File root = new File(tmpDir);
        mkdirOrThrow(root);
        
        // Create system-wide configuration
        File sysRoot = new File(root, "system");
        mkdirOrThrow(sysRoot);
        File etc = makeEtc(sysRoot);
        setupSystemAgentConfig(etc, sysProps);
        setupAgentAuth(etc);
        
        // Create user-specific configuration
        File userRoot = new File(root, "user");
        mkdirOrThrow(userRoot);
        System.setProperty("USER_THERMOSTAT_HOME", userRoot.getAbsolutePath());
        File userEtc = makeEtc(userRoot);
        setupUserAgentConfig(userEtc, userProps);

        return tmpDir;
    }

    public static File getAgentConfFile() {
    	return sysAgentConf;
    }

    public static File getUserAgentConfFile() {
        return userAgentConf;
    }

    public static File getAgentAuthFile() {
    	return agentAuth;
    }
    
    public static void deleteRecursively(File root) throws IOException {
        if (root.isFile()) {
            root.delete();
            return;
        } else if (root.isDirectory()) {
            File[] children = root.listFiles();
            for (File child : children) {
                deleteRecursively(child);
            }
            root.delete();
            return;
        } else {
            throw new IOException("Asked to delete but not file or directory" + root.getPath());
        }
    }

    private static File makeEtc(File sysRoot) throws IOException {
        File etc = new File(sysRoot, "etc");
        mkdirOrThrow(etc);
        return etc;
    }

    private static void setupSystemAgentConfig(File etc, Properties agentProperties) throws FileNotFoundException, IOException {
        sysAgentConf = new File(etc, "agent.properties");

        try (OutputStream propsOutputStream = new FileOutputStream(sysAgentConf)) {
            agentProperties.store(propsOutputStream, "thermostat agent test properties");
        }
    }

    private static void setupAgentAuth(File etc) throws IOException {
        agentAuth = new File(etc, "agent.auth");
        FileWriter authWriter = new FileWriter(agentAuth);
        authWriter.append("username=user\npassword=pass\n");
        authWriter.flush();
        authWriter.close();
    }

    private static void setupUserAgentConfig(File etc, Properties agentProperties) throws IOException {
        userAgentConf = new File(etc, "agent.properties");
        
        try (OutputStream propsOutputStream = new FileOutputStream(userAgentConf)) {
            agentProperties.store(propsOutputStream, "thermostat agent test properties");
        }
    }
    
    private static void mkdirOrThrow(File dir) throws IOException {
        if (!dir.mkdirs()) {
            throw new IOException("Failed to create user configuration directory");
        }
    }
}

