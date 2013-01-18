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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

import com.redhat.thermostat.numa.common.NumaNodeStat;

public class NumaStatBuilderTest {

    private static final String TEST_STAT = "numa_hit 32931\n" +
    		"numa_miss 819\n" +
    		"numa_foreign 918\n" +
    		"interleave_hit 5704\n" +
    		"local_node 3293\n" +
    		"other_node 1819\n";

    @Test
    public void test() throws IOException {
        Reader in = new StringReader(TEST_STAT);
        NumaStatBuilder builder = new NumaStatBuilder(in);
        NumaNodeStat stat = builder.build();
        assertEquals(32931L, stat.getNumaHit());
        assertEquals(819L, stat.getNumaMiss());
        assertEquals(918L, stat.getNumaForeign());
        assertEquals(5704L, stat.getInterleaveHit());
        assertEquals(3293L, stat.getLocalNode());
        assertEquals(1819L, stat.getOtherNode());
    }

}
