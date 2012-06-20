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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

class HeapDAOImpl implements HeapDAO {

    private Storage storage;

    HeapDAOImpl(Storage storage) {
        this.storage = storage;
        storage.createConnectionKey(heapInfoCategory);
    }

    @Override
    public void putHeapInfo(HeapInfo heapInfo, InputStream heapDumpData, InputStream histogramData) {
        VmRef vm = heapInfo.getVm();
        Chunk chunk = new Chunk(heapInfoCategory, false);
        
        chunk.put(Key.AGENT_ID, vm.getAgent().getStringID());
        chunk.put(Key.VM_ID, vm.getId());
        chunk.put(Key.TIMESTAMP, heapInfo.getTimestamp());
        String heapDumpId = "heapdump-" + vm.getAgent().getStringID() + "-" + vm.getId() + "-" + heapInfo.getTimestamp();
        String histogramId = "histogram-" + vm.getAgent().getStringID() + "-" + vm.getId() + "-" + heapInfo.getTimestamp();
        if (heapDumpData != null) {
            chunk.put(heapDumpIdKey, heapDumpId);
            heapInfo.setHeapDumpId(heapDumpId);
        }
        if (histogramData != null) {
            chunk.put(histogramIdKey, histogramId);
            heapInfo.setHistogramId(histogramId);
        }
        storage.putChunk(chunk);
        if (heapDumpData != null) {
            storage.saveFile(heapDumpId, heapDumpData);
        }
        if (histogramData != null) {
            storage.saveFile(histogramId, histogramData);
        }
    }

    @Override
    public Collection<HeapInfo> getAllHeapInfo(VmRef vm) {

        Chunk query = new Chunk(heapInfoCategory, false);
        query.put(Key.AGENT_ID, vm.getAgent().getAgentId());
        query.put(Key.VM_ID, vm.getId());
        Cursor cursor = storage.findAll(query);
        Collection<HeapInfo> heapInfos = new ArrayList<>();
        while (cursor.hasNext()) {
            heapInfos.add(convertChunkToHeapInfo(vm, cursor.next()));
        }
        return heapInfos;
    }

    private HeapInfo convertChunkToHeapInfo(VmRef vm, Chunk chunk) {
        HeapInfo info = new HeapInfo(vm, chunk.get(Key.TIMESTAMP));
        info.setHeapDumpId(chunk.get(HeapDAO.heapDumpIdKey));
        return info;
    }

    @Override
    public InputStream getHeapDump(HeapInfo heapInfo) {
        return storage.loadFile(heapInfo.getHeapDumpId());
    }

    @Override
    public InputStream getHistogram(HeapInfo heapInfo) {
        return storage.loadFile(heapInfo.getHistogramId());
    }

}
