package com.redhat.thermostat.common.dao;

import java.io.InputStream;

import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

class HeapDAOImpl implements HeapDAO {

    private Storage storage;

    HeapDAOImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void putHeapInfo(HeapInfo heapInfo) {
        VmRef vm = heapInfo.getVm();
        Chunk chunk = new Chunk(heapInfoCategory, false);
        
        chunk.put(Key.AGENT_ID, vm.getAgent().getStringID());
        chunk.put(Key.VM_ID, vm.getId());
        chunk.put(Key.TIMESTAMP, heapInfo.getTimestamp());
        InputStream heapDumpData = heapInfo.getHeapDump();
        String heapDumpId = "heapdump-" + vm.getAgent().getStringID() + "-" + vm.getId() + "-" + heapInfo.getTimestamp();
        if (heapDumpData != null) {
            chunk.put(heapDumpIdKey, heapDumpId);
        }
        storage.createConnectionKey(heapInfoCategory);
        storage.putChunk(chunk);
        if (heapDumpData != null) {
            storage.saveFile(heapDumpId, heapDumpData);
        }
    }

}
