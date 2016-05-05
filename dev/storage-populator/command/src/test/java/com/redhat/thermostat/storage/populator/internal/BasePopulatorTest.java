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

package com.redhat.thermostat.storage.populator.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;

public class BasePopulatorTest {

    private BasePopulator populator;

    private ByteArrayOutputStream outputBAOS;
    private PrintStream output;
    private Console console;

    @Before
    public void setUp() {
        populator = createBasePopulatorForTest();

        console = mock(Console.class);
        outputBAOS = new ByteArrayOutputStream();
        output = new PrintStream(outputBAOS);
        when(console.getOutput()).thenReturn(output);
    }

    private BasePopulator createBasePopulatorForTest() {
        return new BasePopulator() {
            private int currCount = 0;

            @Override
            long getCount() {
                currCount++;
                return currCount;
            }

            @Override
            public SharedState addPojos(ConfigItem item, SharedState relState, Console console) {
                return null;
            }

            @Override
            public String getHandledCollection() {
                return null;
            }
        };
    }

    @Test
    public void testDoWaitUntilCount() {
        final long expectedCount = 3;

        populator.doWaitUntilCount(expectedCount, console, 0);

        String expected = "Waiting for storage items to arrive at backend...";
        for (int i = 0; i < (expectedCount - 1); i++) {
            expected = expected + ".";
        }
        expected = expected + "Items have arrived.\n";

        String outputString = new String(outputBAOS.toByteArray());
        assertEquals(expected, outputString);
    }

    @Test
    public void testReportSubmitted() {
        final String name  = "foo";
        final int totalCount = 3;

        ConfigItem item = mock(ConfigItem.class);
        when(item.getName()).thenReturn(name);

        populator.reportSubmitted(item, totalCount, console);

        String expectedString  = "Submitted " + totalCount + " " + name + " records to storage.\n";
        String outputString = new String(outputBAOS.toByteArray());
        assertEquals(expectedString, outputString);
    }
}
