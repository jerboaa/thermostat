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

package com.redhat.thermostat.common.dao;

import java.util.List;
import java.util.Map;

import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmInfoConverter implements Converter<VmInfo> {

    @Override
    public Chunk toChunk(VmInfo info) {
        Chunk chunk = new Chunk(VmInfoDAO.vmInfoCategory, true);

        chunk.put(Key.VM_ID, info.getVmId());
        chunk.put(VmInfoDAO.vmPidKey, info.getVmPid());
        chunk.put(VmInfoDAO.startTimeKey, info.getStartTimeStamp());
        chunk.put(VmInfoDAO.stopTimeKey, info.getStopTimeStamp());
        chunk.put(VmInfoDAO.runtimeVersionKey, info.getJavaVersion());
        chunk.put(VmInfoDAO.javaHomeKey, info.getJavaHome());
        chunk.put(VmInfoDAO.mainClassKey, info.getMainClass());
        chunk.put(VmInfoDAO.commandLineKey, info.getJavaCommandLine());
        chunk.put(VmInfoDAO.vmNameKey, info.getVmName());
        chunk.put(VmInfoDAO.vmInfoKey, info.getVmInfo());
        chunk.put(VmInfoDAO.vmVersionKey, info.getVmVersion());
        chunk.put(VmInfoDAO.vmArgumentsKey, info.getVmArguments());
        chunk.put(VmInfoDAO.propertiesKey, info.getProperties());
        chunk.put(VmInfoDAO.environmentKey, info.getEnvironment());
        chunk.put(VmInfoDAO.librariesKey, info.getLoadedNativeLibraries());
        return chunk;
    }

    @Override
    public VmInfo fromChunk(Chunk chunk) {
        int vmId = chunk.get(Key.VM_ID);
        long startTime = chunk.get(VmInfoDAO.startTimeKey);
        long stopTime = chunk.get(VmInfoDAO.stopTimeKey);
        String jVersion = chunk.get(VmInfoDAO.runtimeVersionKey);
        String jHome = chunk.get(VmInfoDAO.javaHomeKey);
        String mainClass = chunk.get(VmInfoDAO.mainClassKey);
        String commandLine = chunk.get(VmInfoDAO.commandLineKey);
        String vmName = chunk.get(VmInfoDAO.vmNameKey);
        String vmInfo = chunk.get(VmInfoDAO.vmInfoKey);
        String vmVersion = chunk.get(VmInfoDAO.vmVersionKey);
        String vmArgs = chunk.get(VmInfoDAO.vmArgumentsKey);
        Map<String, String> props = chunk.get(VmInfoDAO.propertiesKey);
        Map<String, String> env = chunk.get(VmInfoDAO.propertiesKey);
        List<String> libs = chunk.get(VmInfoDAO.librariesKey);
        return new VmInfo(vmId, startTime, stopTime, jVersion, jHome, mainClass, commandLine, vmName,
                vmInfo, vmVersion, vmArgs, props, env, libs);
    }
}
