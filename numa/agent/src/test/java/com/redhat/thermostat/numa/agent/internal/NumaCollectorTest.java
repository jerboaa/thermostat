/*
 * Copyright 2013 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.numa.common.NumaStat;

public class NumaCollectorTest {

    private static final String TEST_STAT0 = "numa_hit 11\n" +
            "numa_miss 12\n" +
            "numa_foreign 13\n" +
            "interleave_hit 14\n" +
            "local_node 15\n" +
            "other_node 16\n";

    private static final String TEST_STAT1 = "numa_hit 21\n" +
            "numa_miss 22\n" +
            "numa_foreign 23\n" +
            "interleave_hit 24\n" +
            "local_node 25\n" +
            "other_node 26\n";

    private static final String TEST_STAT2 = "numa_hit 31\n" +
            "numa_miss 32\n" +
            "numa_foreign 33\n" +
            "interleave_hit 34\n" +
            "local_node 35\n" +
            "other_node 36\n";

    private File tmpDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("numa-test").toFile();

        File nodeDir0 = new File(tmpDir, "node0");
        nodeDir0.mkdir();
        File numaStatFile0 = new File(nodeDir0, "numastat");
        FileOutputStream out0 = new FileOutputStream(numaStatFile0);
        out0.write(TEST_STAT0.getBytes());
        out0.close();

        File nodeDir1 = new File(tmpDir, "node1");
        nodeDir1.mkdir();
        File numaStatFile1 = new File(nodeDir1, "numastat");
        FileOutputStream out1 = new FileOutputStream(numaStatFile1);
        out1.write(TEST_STAT1.getBytes());
        out1.close();

        File nodeDir2 = new File(tmpDir, "node2");
        nodeDir2.mkdir();
        File numaStatFile2 = new File(nodeDir2, "numastat");
        FileOutputStream out2 = new FileOutputStream(numaStatFile2);
        out2.write(TEST_STAT2.getBytes());
        out2.close();

        File fluff = new File(tmpDir, "fluff");
        fluff.mkdir();

    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(tmpDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Test
    public void testStats() throws IOException {
        NumaCollector coll = new NumaCollector(tmpDir.getAbsolutePath());
        NumaStat[] stats = coll.collectData();

        assertEquals(3, stats.length);

        assertEquals(0, stats[0].getNode());
        assertEquals(11, stats[0].getNumaHit());
        assertEquals(12, stats[0].getNumaMiss());
        assertEquals(13, stats[0].getNumaForeign());
        assertEquals(14, stats[0].getInterleaveHit());
        assertEquals(15, stats[0].getLocalNode());
        assertEquals(16, stats[0].getOtherNode());

        assertEquals(1, stats[1].getNode());
        assertEquals(21, stats[1].getNumaHit());
        assertEquals(22, stats[1].getNumaMiss());
        assertEquals(23, stats[1].getNumaForeign());
        assertEquals(24, stats[1].getInterleaveHit());
        assertEquals(25, stats[1].getLocalNode());
        assertEquals(26, stats[1].getOtherNode());

        assertEquals(2, stats[2].getNode());
        assertEquals(31, stats[2].getNumaHit());
        assertEquals(32, stats[2].getNumaMiss());
        assertEquals(33, stats[2].getNumaForeign());
        assertEquals(34, stats[2].getInterleaveHit());
        assertEquals(35, stats[2].getLocalNode());
        assertEquals(36, stats[2].getOtherNode());

    }

    @Test
    public void testDefaultDir() {
        NumaCollector coll = new NumaCollector();
        assertEquals("/sys/devices/system/node", coll.getBaseDir());
    }
}
