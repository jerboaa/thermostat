package com.redhat.thermostat.common.dao;

import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Chunk;

interface Converter<T extends Pojo> {

    Chunk toChunk(T pojo);

    T fromChunk(Chunk chunk);
}
