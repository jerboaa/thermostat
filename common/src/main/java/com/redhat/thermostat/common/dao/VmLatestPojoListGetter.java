package com.redhat.thermostat.common.dao;

import com.redhat.thermostat.common.model.TimeStampedPojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

class VmLatestPojoListGetter<T extends TimeStampedPojo> extends HostLatestPojoListGetter<T> {

    private VmRef vmRef;

    VmLatestPojoListGetter(Storage storage, Category cat, Converter<T> converter, VmRef ref) {
        super(storage, cat, converter, ref.getAgent());
        vmRef = ref;
    }

    @Override
    protected Chunk buildQuery() {
        Chunk query = super.buildQuery();
        query.put(Key.VM_ID, vmRef.getId());
        return query;
    }
}
