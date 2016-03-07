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

package com.redhat.thermostat.storage.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Countable;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.model.VmInfo.KeyValuePair;

@Service
public interface VmInfoDAO extends Countable {

    static final Key<Integer> vmPidKey = new Key<>("vmPid");
    static final Key<String> runtimeVersionKey = new Key<>("javaVersion");
    static final Key<String> javaHomeKey = new Key<>("javaHome");
    static final Key<String> mainClassKey = new Key<>("mainClass");
    static final Key<String> commandLineKey = new Key<>("javaCommandLine");
    static final Key<String> vmArgumentsKey = new Key<>("vmArguments");
    static final Key<String> vmNameKey = new Key<>("vmName");
    static final Key<String> vmInfoKey = new Key<>("vmInfo");
    static final Key<String> vmVersionKey = new Key<>("vmVersion");
    static final Key<KeyValuePair[]> propertiesKey = new Key<>("propertiesAsArray");
    static final Key<KeyValuePair[]> environmentKey = new Key<>("environmentAsArray");
    static final Key<String[]> librariesKey = new Key<>("loadedNativeLibraries");
    static final Key<Long> startTimeKey = new Key<>("startTimeStamp");
    static final Key<Long> stopTimeKey = new Key<>("stopTimeStamp");
    static final Key<Long> uidKey = new Key<>("uid");
    static final Key<String> usernameKey = new Key<>("username");

    static final Category<VmInfo> vmInfoCategory = new Category<>("vm-info", VmInfo.class,
            Key.AGENT_ID, Key.VM_ID, vmPidKey, runtimeVersionKey, javaHomeKey,
            mainClassKey, commandLineKey,
            vmArgumentsKey, vmNameKey, vmInfoKey, vmVersionKey,
            propertiesKey, environmentKey, librariesKey,
            startTimeKey, stopTimeKey,
            uidKey, usernameKey);

    /** @return information on all known VMs */
    List<VmInfo> getAllVmInfos();

    /** @return {@code null} if no information can be found */
    VmInfo getVmInfo(VmId id);

    /** @return {@code null} if no information can be found */
    VmInfo getVmInfo(VmRef ref);

    /**
     *
     * @param host The host to get the VM(s) for.
     * @return A collection of the VM(s) as VmRef(s).
     *
     * @deprecated use {@link #getVmIds(AgentId)}
     */
    @Deprecated
    Collection<VmRef> getVMs(HostRef host);

    /**
     *
     * @param agentId The id of host to get the VM(s) for.
     * @return A set of the VmId(s).
     */
    Set<VmId> getVmIds(AgentId agentId);

    void putVmInfo(VmInfo info);

    void putVmStoppedTime(String vmId, long since);
}

