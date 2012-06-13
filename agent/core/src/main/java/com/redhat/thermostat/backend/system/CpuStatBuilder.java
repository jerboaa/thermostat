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

package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class CpuStatBuilder {

    private static final Logger logger = LoggingUtils.getLogger(CpuStatBuilder.class);

    private final ProcDataSource dataSource;

    public CpuStatBuilder(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CpuStat build() {
        try (BufferedReader reader = new BufferedReader(dataSource.getCpuLoadReader())) {
            return build(reader);
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read data source for cpu info");
        }
        return new CpuStat(System.currentTimeMillis(),
                CpuStat.INVALID_LOAD, CpuStat.INVALID_LOAD, CpuStat.INVALID_LOAD);
    }

    private CpuStat build(BufferedReader reader) throws IOException {
        long timestamp = System.currentTimeMillis();
        double load5 = CpuStat.INVALID_LOAD;
        double load10 = CpuStat.INVALID_LOAD;
        double load15 = CpuStat.INVALID_LOAD;
        String[] loadAvgParts = reader.readLine().split(" +");
        if (loadAvgParts.length >= 3) {
            try {
                load5 = Double.valueOf(loadAvgParts[0]);
                load10 = Double.valueOf(loadAvgParts[1]);
                load15 = Double.valueOf(loadAvgParts[2]);
            } catch (NumberFormatException nfe) {
                logger.log(Level.WARNING, "error extracting load");
            }
        }
        return new CpuStat(timestamp, load5, load10, load15);
    }
}
