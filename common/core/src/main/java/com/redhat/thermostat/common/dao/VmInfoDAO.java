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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.VmInfo;

public interface VmInfoDAO extends Countable {

    static final Key<Integer> vmPidKey = new Key<>("vmPid", false);
    static final Key<String> runtimeVersionKey = new Key<>("javaVersion", false);
    static final Key<String> javaHomeKey = new Key<>("javaHome", false);
    static final Key<String> mainClassKey = new Key<>("mainClass", false);
    static final Key<String> commandLineKey = new Key<>("javaCommandLine", false);
    static final Key<String> vmArgumentsKey = new Key<>("vmArguments", false);
    static final Key<String> vmNameKey = new Key<>("vmName", false);
    static final Key<String> vmInfoKey = new Key<>("vmInfo", false);
    static final Key<String> vmVersionKey = new Key<>("vmVersion", false);
    static final Key<Map<String, String>> propertiesKey = new Key<>("properties", false);
    static final Key<Map<String, String>> environmentKey = new Key<>("environment", false);
    static final Key<List<String>> librariesKey = new Key<>("loadedNativeLibraries", false);
    static final Key<Long> startTimeKey = new Key<>("startTimeStamp", false);
    static final Key<Long> stopTimeKey = new Key<>("stopTimeStamp", false);

    static final Category<VmInfo> vmInfoCategory = new Category<>("vm-info", VmInfo.class,
            Key.AGENT_ID, Key.VM_ID, vmPidKey, runtimeVersionKey, javaHomeKey,
            mainClassKey, commandLineKey,
            vmArgumentsKey, vmNameKey, vmInfoKey, vmVersionKey,
            propertiesKey, environmentKey, librariesKey,
            startTimeKey, stopTimeKey);

    public VmInfo getVmInfo(VmRef ref);

    Collection<VmRef> getVMs(HostRef host);

    public void putVmInfo(VmInfo info);

    public void putVmStoppedTime(int vmId, long since);
}
