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

import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;

public interface VmMemoryStatDAO {

    static final Key<String> edenGenKey = new Key<>("eden.gen", false);
    static final Key<String> edenCollectorKey = new Key<>("eden.collector", false);
    static final Key<Long> edenCapacityKey = new Key<>("eden.capacity", false);
    static final Key<Long> edenMaxCapacityKey = new Key<>("eden.max-capacity", false);
    static final Key<Long> edenUsedKey = new Key<>("eden.used", false);

    static final Key<String> s0GenKey = new Key<>("s0.gen", false);
    static final Key<String> s0CollectorKey = new Key<>("s0.collector", false);
    static final Key<Long> s0CapacityKey = new Key<>("s0.capacity", false);
    static final Key<Long> s0MaxCapacityKey = new Key<>("s0.max-capacity", false);
    static final Key<Long> s0UsedKey = new Key<>("s0.used", false);

    static final Key<String> s1GenKey = new Key<>("s1.gen", false);
    static final Key<String> s1CollectorKey = new Key<>("s1.collector", false);
    static final Key<Long> s1CapacityKey = new Key<>("s1.capacity", false);
    static final Key<Long> s1MaxCapacityKey = new Key<>("s1.max-capacity", false);
    static final Key<Long> s1UsedKey = new Key<>("s1.used", false);

    static final Key<String> oldGenKey = new Key<>("old.gen", false);
    static final Key<String> oldCollectorKey = new Key<>("old.collector", false);
    static final Key<Long> oldCapacityKey = new Key<>("old.capacity", false);
    static final Key<Long> oldMaxCapacityKey = new Key<>("old.max-capacity", false);
    static final Key<Long> oldUsedKey = new Key<>("old.used", false);

    static final Key<String> permGenKey = new Key<>("perm.gen", false);
    static final Key<String> permCollectorKey = new Key<>("perm.collector", false);
    static final Key<Long> permCapacityKey = new Key<>("perm.capacity", false);
    static final Key<Long> permMaxCapacityKey = new Key<>("perm.max-capacity", false);
    static final Key<Long> permUsedKey = new Key<>("perm.used", false);

    static final Category vmMemoryStatsCategory = new Category("vm-memory-stats",
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            edenGenKey, edenCollectorKey,
            edenCapacityKey, edenMaxCapacityKey,edenUsedKey,
            s0GenKey, s0CollectorKey, s0CapacityKey,
            s0MaxCapacityKey, s0UsedKey,
            s1GenKey, s1CollectorKey, s1CapacityKey,
            s1MaxCapacityKey, s1UsedKey,
            oldGenKey, oldCollectorKey, oldCapacityKey,
            oldMaxCapacityKey, oldUsedKey,
            permGenKey, permCollectorKey, permCapacityKey,
            permMaxCapacityKey, permUsedKey);

    public VmMemoryStat getLatestMemoryStat(VmRef ref);

    public void putVmMemoryStat(VmMemoryStat stat);

}
