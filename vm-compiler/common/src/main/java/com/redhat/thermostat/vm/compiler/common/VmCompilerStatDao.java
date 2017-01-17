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

package com.redhat.thermostat.vm.compiler.common;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;

@Service
public interface VmCompilerStatDao {

    static final Key<Long> totalCompilesKey = new Key<>("totalCompiles");
    static final Key<Long> totalBailoutsKey = new Key<>("totalBailouts");
    static final Key<Long> totalInvalidatesKey = new Key<>("totalInvalidates");
    static final Key<Long> compilationTimeKey = new Key<>("compilationTime");
    static final Key<Long> lastSizeKey = new Key<>("lastSize");
    static final Key<Long> lastTypeKey = new Key<>("lastType");
    static final Key<String> lastMethodKey = new Key<>("lastMethod");
    static final Key<Long> lastFailedTypeKey = new Key<>("lastFailedType");
    static final Key<String> lastFailedMethodKey = new Key<>("lastFailedMethod");


    static final Category<VmCompilerStat> vmCompilerStatsCategory = new Category<>(
            "vm-compiler-stats", VmCompilerStat.class,
            Arrays.<Key<?>>asList(
                    Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
                    totalCompilesKey, totalBailoutsKey, totalInvalidatesKey,
                    compilationTimeKey,
                    lastSizeKey, lastTypeKey, lastMethodKey,
                    lastFailedTypeKey, lastFailedMethodKey),
            Arrays.<Key<?>>asList(Key.TIMESTAMP));

    public List<VmCompilerStat> getLatestCompilerStats(VmRef ref, long since);

    public List<VmCompilerStat> getCompilerStats(VmRef ref, long since, long to);

    public void putVmCompilerStat(VmCompilerStat stat);

    public abstract VmCompilerStat getOldest(VmRef ref);

    public abstract VmCompilerStat getNewest(VmRef ref);

}

