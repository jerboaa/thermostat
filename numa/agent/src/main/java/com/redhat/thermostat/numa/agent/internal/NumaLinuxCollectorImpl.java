/*
 * Copyright 2012-2016 Red Hat, Inc.
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


package com.redhat.thermostat.numa.agent.internal;

import com.redhat.thermostat.numa.common.NumaNodeStat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

class NumaLinuxCollectorImpl implements NumaCollector {

    private static final String NUMA_BASE_DIR = "/sys/devices/system/node";

    private static final String NODE_DIR_PREFIX = "node";

    private static final String NUMA_STAT_FILE = "numastat";

    private int numberOfNodes;

    private File baseDir;

    NumaLinuxCollectorImpl() {
        this(NUMA_BASE_DIR);
    }

    NumaLinuxCollectorImpl(String baseDirectory) {
        baseDir = new File(baseDirectory);
        FilenameFilter filter = new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(NODE_DIR_PREFIX);
            }
        };
        String[] nodeFiles = baseDir.list(filter);
        numberOfNodes = nodeFiles.length;
    }

    public NumaNodeStat[] collectData() throws IOException {
        NumaNodeStat[] stat = new NumaNodeStat[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            File nodeDir = new File(baseDir, NODE_DIR_PREFIX + i);
            File numaStatFile = new File(nodeDir, NUMA_STAT_FILE);
            try (FileInputStream in = new FileInputStream(numaStatFile)) {
                Reader reader = new InputStreamReader(in);
                NumaStatBuilder builder = new NumaStatBuilder(reader);
                stat[i] = builder.build();
                stat[i].setNodeId(i);
            }
        }
        return stat;
    }

    // This is here for testing.
    String getBaseDir() {
        return baseDir.getAbsolutePath();
    }

    public int getNumberOfNumaNodes() {
        return numberOfNodes;
    }
}

