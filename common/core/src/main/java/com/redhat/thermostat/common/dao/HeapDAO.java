package com.redhat.thermostat.common.dao;

import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;

public interface HeapDAO {

    static final Key<String> heapDumpIdKey = new Key<String>("heap-dump-id", false);

    public static final Category heapInfoCategory = new Category("vm-heap-info", Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, heapDumpIdKey);

    void putHeapInfo(HeapInfo heapInfo);

}
