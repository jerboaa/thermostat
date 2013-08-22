/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

@Service
public interface HeapDAO {

    static final Key<String> heapIdKey = new Key<String>("heapId");
    static final Key<String> heapDumpIdKey = new Key<String>("heapDumpId");
    static final Key<String> histogramIdKey = new Key<String>("histogramId");

    public static final Category<HeapInfo> heapInfoCategory = new Category<>("vm-heap-info", HeapInfo.class, Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, heapIdKey, heapDumpIdKey, histogramIdKey);

    void putHeapInfo(HeapInfo heapInfo, File heapDumpFile, ObjectHistogram histogramData) throws IOException;

    Collection<HeapInfo> getAllHeapInfo(VmRef vm);

    InputStream getHeapDumpData(HeapInfo heapInfo);

    ObjectHistogram getHistogram(HeapInfo heapInfo);

    HeapInfo getHeapInfo(String heapId);

    HeapDump getHeapDump(HeapInfo heapInfo);

}

