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
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;

public interface VmInfoDAO {

    static final Key<Integer> vmIdKey = new Key<>("vm-id", true);
    static final Key<Integer> vmPidKey = new Key<>("vm-pid", false);
    static final Key<String> runtimeVersionKey = new Key<>("runtime-version", false);
    static final Key<String> javaHomeKey = new Key<>("java-home", false);
    static final Key<String> mainClassKey = new Key<>("main-class", false);
    static final Key<String> commandLineKey = new Key<>("command-line", false);
    static final Key<String> vmArgumentsKey = new Key<>("vm-arguments", false);
    static final Key<String> vmNameKey = new Key<>("vm-name", false);
    static final Key<String> vmInfoKey = new Key<>("vm-info", false);
    static final Key<String> vmVersionKey = new Key<>("vm-version", false);
    static final Key<Map<String, String>> propertiesKey = new Key<>("properties", false);
    static final Key<Map<String, String>> environmentKey = new Key<>("environment", false);
    static final Key<List<String>> librariesKey = new Key<>("libraries", false);
    static final Key<Long> startTimeKey = new Key<>("start-time", false);
    static final Key<Long> stopTimeKey = new Key<>("stop-time", false);

    static final Category vmInfoCategory = new Category("vm-info",
            vmIdKey, vmPidKey, runtimeVersionKey, javaHomeKey,
            mainClassKey, commandLineKey,
            vmArgumentsKey, vmNameKey, vmInfoKey, vmVersionKey,
            propertiesKey, environmentKey, librariesKey,
            startTimeKey, stopTimeKey);

    public VmInfo getVmInfo(VmRef ref);
}
