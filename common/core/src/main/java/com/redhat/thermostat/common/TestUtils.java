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

package com.redhat.thermostat.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Random;

public class TestUtils {

    public static int getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pidPart = name.split("@")[0];
        return Integer.parseInt(pidPart);
    }

    public static boolean isLinux() {
        return (System.getProperty("os.name").toLowerCase().contains("linux"));
    }
        
    public static String setupAgentConfigs() throws IOException {
        // need to create dummy config files for the tests
        Random random = new Random();

        String tmpDir = System.getProperty("java.io.tmpdir") + File.separatorChar +
                Math.abs(random.nextInt()) + File.separatorChar;

        System.setProperty("THERMOSTAT_HOME", tmpDir);
        File agent = new File(tmpDir, "agent");
        agent.mkdirs();

        File tmpConfigs = new File(agent, "agent.properties");

        new File(agent, "run").mkdirs();
        new File(agent, "logs").mkdirs();

        File backends = new File(tmpDir, "backends");
        File system = new File(backends, "system");
        system.mkdirs();

        Properties props = new Properties();
        
        props.setProperty("SAVE_ON_EXIT", "true");
        props.setProperty("CONFIG_LISTEN_ADDRESS", "42.42.42.42:42");
        
        props.store(new FileOutputStream(tmpConfigs), "thermostat agent test properties");
        
        return tmpDir;
    }
}
