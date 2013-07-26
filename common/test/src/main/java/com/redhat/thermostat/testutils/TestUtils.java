/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
     */
    public static String setupAgentConfigs(Properties agentProperties) throws IOException {
        // need to create dummy config files for the tests
        Random random = new Random();

        String tmpDir = System.getProperty("java.io.tmpdir") + File.separatorChar +
                Math.abs(random.nextInt()) + File.separatorChar;

        setupSystemAgentConfig(tmpDir, agentProperties);
        setupUserAgentConfig(tmpDir);

        return tmpDir;
    }

    private static void setupSystemAgentConfig(String root, Properties agentProperties) throws FileNotFoundException, IOException {
        System.setProperty("THERMOSTAT_HOME", root);

        File etc = new File(root, "etc");
        etc.mkdirs();
        File tmpConfigs = new File(etc, "agent.properties");

        try (OutputStream propsOutputStream = new FileOutputStream(tmpConfigs)) {
            agentProperties.store(propsOutputStream, "thermostat agent test properties");
        }

        File tmpAuth = new File(etc, "agent.auth");
        FileWriter authWriter = new FileWriter(tmpAuth);
        authWriter.append("username=user\npassword=pass\n");
        authWriter.flush();
        authWriter.close();
    }

    private static void setupUserAgentConfig(String root) throws IOException {
        System.setProperty("USER_THERMOSTAT_HOME", root);

        File agent = new File(root, "agent");
        agent.mkdirs();

        new File(agent, "run").mkdirs();
        new File(agent, "logs").mkdirs();

    }
}

