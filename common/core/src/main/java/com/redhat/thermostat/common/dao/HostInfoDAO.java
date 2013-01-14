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

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.HostInfo;

public interface HostInfoDAO extends Countable {

    static Key<String> hostNameKey = new Key<>("hostname", true);
    static Key<String> osNameKey = new Key<>("osName", false);
    static Key<String> osKernelKey = new Key<>("osKernel", false);
    static Key<Integer> cpuCountKey = new Key<>("cpuCount", false);
    static Key<String> cpuModelKey = new Key<>("cpuModel", false);
    static Key<Long> hostMemoryTotalKey = new Key<>("totalMemory", false);

    static final Category<HostInfo> hostInfoCategory = new Category<>("host-info", HostInfo.class,
            Key.AGENT_ID, hostNameKey, osNameKey, osKernelKey,
            cpuCountKey, cpuModelKey, hostMemoryTotalKey);

    HostInfo getHostInfo(HostRef ref);

    void putHostInfo(HostInfo info);

    Collection<HostRef> getHosts();
    Collection<HostRef> getAliveHosts();
}
