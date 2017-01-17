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

package com.redhat.thermostat.client.cli;

import java.util.List;

import com.redhat.thermostat.annotations.ExtensionPoint;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

/**
 * This interface should be implemented by plug-ins that would like to
 * contribute data to the output of the vm-stat command.
 */
@ExtensionPoint
public interface VMStatPrintDelegate extends Ordered {
    
    /**
     * Returns statistics gathered by this plug-in newer than the specified
     * time stamp.
     * @param ref - the VM whose statistics to return
     * @param timeStampSince - the earliest time stamp to return statistics for
     * @return a list of statistics newer than the time stamp
     *
     * @deprecated use {@link #getLatestStats(AgentId, VmId, long)} instead.
     */
    @Deprecated
    public List<? extends TimeStampedPojo> getLatestStats(VmRef ref, long timeStampSince);

    /**
     * Returns statistics gathered by this plug-in newer than the specified
     * time stamp.
     * @param agentId - the ID of the monitoring agent
     * @param vmId - the ID of the vm whose statistics to return
     * @param timeStampSince - the earliest time stamp to return statistics for
     * @return a list of statistics newer than the time stamp
     */
    public List<? extends TimeStampedPojo> getLatestStats(AgentId agentId, VmId vmId, long timeStampSince);
    
    /**
     * Returns header names for columns this plug-in wishes to add to the 
     * vm-stat command.
     * @param stat - the first stat returned by {@link #getLatestStats(AgentId, VmId, long)}
     * @return a list of column headers to append to vm-stat output
     */
    public List<String> getHeaders(TimeStampedPojo stat);
    
    /**
     * Returns a row of data for the specified statistic that corresponds to
     * the columns returned by {@link #getHeaders(TimeStampedPojo)}.
     * @param stat - the statistic to generate output for
     * @return a row of text for this statistic separated by column
     */
    public List<String> getStatRow(TimeStampedPojo stat);
    
}

