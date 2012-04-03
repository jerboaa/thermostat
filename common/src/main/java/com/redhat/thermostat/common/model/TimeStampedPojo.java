package com.redhat.thermostat.common.model;

/**
 * Any Pojo which is taken as a timestamped piece of data should
 * implement this interface.
 */
public interface TimeStampedPojo extends Pojo {

    public long getTimeStamp();

}
