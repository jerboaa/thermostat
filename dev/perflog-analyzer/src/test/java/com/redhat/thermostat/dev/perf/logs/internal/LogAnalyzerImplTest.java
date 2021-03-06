/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.dev.perf.logs.Direction;
import com.redhat.thermostat.dev.perf.logs.SortBy;
import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class LogAnalyzerImplTest {
    
    private PrintStream out;
    
    @Before
    public void setup() {
        out = System.out;
    }
    
    @After
    public void tearDown() {
        System.setOut(out);
    }

    @Test
    public void verifyBasicAnalysis() {
        File logFile = new File(decodeFilePath(this.getClass().getResource("/perflogMultipleLogTags.log")));
        StatsConfig config = new StatsConfig(logFile, SortBy.AVG, Direction.DSC, false);
        LogAnalyzerImpl analyzer = new LogAnalyzerImpl(config);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pStream = new PrintStream(baos);
        System.setOut(pStream);
        // This prints to stdout
        analyzer.analyze();
        String output = baos.toString();
        baos.reset();
        // last config value should make the analyzer print more output
        StatsConfig otherConfig = new StatsConfig(logFile, SortBy.AVG, Direction.DSC, true);
        analyzer = new LogAnalyzerImpl(otherConfig);
        analyzer.analyze();
        String moreOutput = baos.toString();
        assertTrue(moreOutput.contains(output));
        assertFalse(moreOutput.equals(output));
    }
    
    private static String decodeFilePath(URL url) {
        try {
            // Spaces are encoded as %20 in URLs. Use URLDecoder.decode() so
            // as to handle cases like that.
            return URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 not supported, huh?");
        }
    }
}
