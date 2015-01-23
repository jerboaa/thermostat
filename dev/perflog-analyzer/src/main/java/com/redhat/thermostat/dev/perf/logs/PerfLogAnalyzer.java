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

package com.redhat.thermostat.dev.perf.logs;

import com.redhat.thermostat.dev.perf.logs.internal.StatsConfigParser;

public class PerfLogAnalyzer {

    static void printUsage() {
        System.err.println("usage: java -cp thermostat-perflog-analyzer*.jar " + PerfLogAnalyzer.class.getName() + " [OPTIONS] <perflog-file>");
        System.err.println("");
        System.err.println("  OPTIONS:");
        System.err.print("   --" + StatsConfig.SORT_KEY + "=<KEY>   <KEY> is one of:");
        SortBy[] sorts = SortBy.values();
        for (int i = 0; i < sorts.length; i++) {
            System.err.print(" " + sorts[i]);
            if (i != sorts.length - 1) {
                System.err.print(",");
            }
        }
        System.err.println(" (" + SortBy.AVG + " is default)");
        System.err.print("   --" + StatsConfig.DIRECTION_KEY + "=<KEY>   <KEY> is one of: ");
        System.err.print(Direction.ASC.name() + ", " + Direction.DSC.name());
        System.err.println(" (" + Direction.DSC + " is default)");
        System.err.println("   --" + StatsConfig.SHOW_BACKING + "   Shows stats analysis for backing storage (if any)");
    }
    
    private static void usage() {
        printUsage();
        System.exit(1);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }
        StatsConfigParser statsConfigParser = new StatsConfigParser(args);
        StatsConfig config = null;
        try {
            config = statsConfigParser.parse();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("");
            usage();
        }
        AnalyzerBuilder builder = AnalyzerBuilder.create();
        LogAnalyzer analyzer = builder.setConfig(config).build();
        analyzer.analyze();
    }

}
