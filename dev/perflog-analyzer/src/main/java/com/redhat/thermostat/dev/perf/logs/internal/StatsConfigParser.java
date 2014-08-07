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

import java.io.File;

import com.redhat.thermostat.dev.perf.logs.Direction;
import com.redhat.thermostat.dev.perf.logs.SortBy;
import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class StatsConfigParser {

    private static final String OPTION_PREFIX = "--";
    static final SortBy DEFAULT_SORT = SortBy.AVG;
    static final Direction DEFAULT_SORT_DIRECTION = Direction.DSC;
    
    private final String[] args;
    private SortBy parsedSortBy;
    private Direction parsedDirection;
    private File parsedFile;
    
    public StatsConfigParser(String[] args) {
        this.args = args;
    }
    
    /**
     * 
     * @return a {@link StatsConfig} instance corresponding to the
     *         String argument list.
     * @throws IllegalArgumentException
     */
    public StatsConfig parse() throws IllegalArgumentException {
        // process options and arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(OPTION_PREFIX)) {
                processOption(args[i].substring(OPTION_PREFIX.length()));
            } else {
                processFile(args[i]);
            }
        }
        setDefaultsPerformSanityChecks();
        return new StatsConfig(parsedFile, parsedSortBy, parsedDirection);
    }

    private void setDefaultsPerformSanityChecks() throws IllegalArgumentException {
        if (parsedDirection == null) {
            parsedDirection = DEFAULT_SORT_DIRECTION; // direction is optional
        }
        if (parsedSortBy == null) {
            parsedSortBy = DEFAULT_SORT; // sort is optional
        }
        if (parsedFile == null) {
            throw new IllegalArgumentException("No log file specified");
        }
    }

    private void processFile(String fileName) throws IllegalArgumentException {
        if (parsedFile != null) {
            throw new IllegalArgumentException("Multiple files specified");
        }
        parsedFile = new File(fileName);
    }

    private void processOption(String keyValue) throws IllegalArgumentException {
        String[] keyValues = keyValue.split("=");
        if (keyValues.length != 2) {
            throw new IllegalArgumentException("Illegal key value pair");
        }
        if (keyValues[0].equals(StatsConfig.SORT_KEY)) {
            parseSortBy(keyValues[1]);
        } else if (keyValues[0].equals(StatsConfig.DIRECTION_KEY)) {
            parseDirection(keyValues[1]);
        } else {
            throw new IllegalArgumentException("Illegal option " + OPTION_PREFIX + keyValue);
        }
    }

    private void parseDirection(String direction) throws IllegalArgumentException {
        if (parsedDirection != null) {
            throw new IllegalArgumentException("Multiple direction values specified");
        }
        try {
            parsedDirection = Direction.valueOf(direction);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Illegal direction value " + direction, e);
        }
    }

    private void parseSortBy(String sortBy) throws IllegalArgumentException {
        if (parsedSortBy != null) {
            throw new IllegalArgumentException("Multiple sort values specified");
        }
        try {
            parsedSortBy = SortBy.valueOf(sortBy);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Illegal sort value " + sortBy, e);
        }
    }
}
