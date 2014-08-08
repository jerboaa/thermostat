/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.redhat.thermostat.dev.perf.logs.Direction;
import com.redhat.thermostat.dev.perf.logs.SortBy;
import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class StatsConfigParserTest {

    /*
     * No options, just the log file.
     */
    @Test
    public void canParseBasic() {
        String fileName = "path/to/foo";
        String[] testArgs = new String[] {
                fileName
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        StatsConfig config = p.parse();
        assertEquals(new File(fileName), config.getLogFile());
        assertEquals(StatsConfigParser.DEFAULT_SORT, config.getSortBy());
        assertEquals(StatsConfigParser.DEFAULT_SORT_DIRECTION, config.getDirection());
    }
    
    /*
     * Unknown option is expected to throw exceptions.
     */
    @Test(expected=IllegalArgumentException.class)
    public void unknownOptionThrowsException() {
        StatsConfigParser p = new StatsConfigParser(new String[] {
                "--unknown-option", "path/to/foo/test.log"
        });
        p.parse();
    }
    
    /*
     * The direction to sort by is configurable, but is optional.
     */
    @Test
    public void sortWithoutDirectionUsesDefaultDirection() {
        String filename = "path/to/foo/test.log";
        StatsConfigParser p = new StatsConfigParser(new String[] {
                "--" + StatsConfig.SORT_KEY + "=COUNT", filename
        });
        StatsConfig config = p.parse();
        assertEquals(new File(filename), config.getLogFile());
        assertEquals(SortBy.COUNT, config.getSortBy());
        assertEquals(StatsConfigParser.DEFAULT_SORT_DIRECTION, config.getDirection());
    }
    
    /*
     * The sort key is configurable, but is optional.
     */
    @Test
    public void directionWithoutSortUsesDefaultSort() {
        String filename = "path/to/foo/test.log";
        StatsConfigParser p = new StatsConfigParser(new String[] {
                "--" + StatsConfig.DIRECTION_KEY + "=ASC", filename
        });
        StatsConfig config = p.parse();
        assertEquals(new File(filename), config.getLogFile());
        assertEquals(StatsConfigParser.DEFAULT_SORT, config.getSortBy());
        assertEquals(Direction.ASC, config.getDirection());
    }
    
    @Test
    public void noShowBackingUsesDefault() {
        String filename = "path/to/foo/test.log";
        StatsConfigParser p = new StatsConfigParser(new String[] {
                filename
        });
        StatsConfig config = p.parse();
        assertEquals(new File(filename), config.getLogFile());
        assertFalse(config.isShowBacking());
    }
    
    @Test
    public void illegalSortValue() {
        StatsConfigParser p = new StatsConfigParser(new String[] {
                "--" + StatsConfig.SORT_KEY + "=unrecognized", "foo.log"
        });
        try {
            p.parse();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal sort value unrecognized", e.getMessage());
        }
    }
    
    @Test
    public void illegalDirectionValue() {
        StatsConfigParser p = new StatsConfigParser(new String[] {
                "--" + StatsConfig.DIRECTION_KEY + "=bar", "foo.log"
        });
        try {
            p.parse();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal direction value bar", e.getMessage());
        }
    }
    
    @Test
    public void allOptionsSpecifiedFileNameLast() {
        String filename = "/path/to/foo.log";
        String[] testArgs = new String[] {
                "--sort-by=MAX",
                "--direction=ASC",
                "--show-backing",
                filename
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        StatsConfig config = p.parse();
        assertEquals(new File(filename), config.getLogFile());
        assertEquals(SortBy.MAX, config.getSortBy());
        assertEquals(Direction.ASC, config.getDirection());
        assertTrue(config.isShowBacking());
    }
    
    @Test
    public void allOptionsSpecifiedFileNameFirst() {
        String filename = "/path/to/foo.log";
        String[] testArgs = new String[] {
                filename,
                "--sort-by=MIN",
                "--direction=DSC",
                "--show-backing"
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        StatsConfig config = p.parse();
        assertEquals(new File(filename), config.getLogFile());
        assertEquals(SortBy.MIN, config.getSortBy());
        assertEquals(Direction.DSC, config.getDirection());
        assertTrue(config.isShowBacking());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void oneArgOption() {
        String filename = "--sort-by=fileName";
        String[] testArgs = new String[] {
                filename,
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        p.parse();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void multipleSortFail() {
        String sort = "--sort-by=MAX";
        String[] testArgs = new String[] {
                sort, "foo.log", sort
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        p.parse();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void multipleShowBackingFail() {
        String showBacking = "--show-backing";
        String[] testArgs = new String[] {
                showBacking, "foo.log", showBacking
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        p.parse();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void multipleDirectionFail() {
        String sort = "--direction=ASC";
        String[] testArgs = new String[] {
                sort, "foo.log", sort
        };
        StatsConfigParser p = new StatsConfigParser(testArgs);
        p.parse();
    }
}
