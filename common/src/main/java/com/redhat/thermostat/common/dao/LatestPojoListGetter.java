package com.redhat.thermostat.common.dao;

import java.util.List;

import com.redhat.thermostat.common.model.Pojo;

interface LatestPojoListGetter<T extends Pojo> {
    List<T> getLatest();
}
