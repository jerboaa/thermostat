package com.redhat.thermostat.client;

/**
 * Represents a data associated with a discrete point in time
 *
 * @param <T> the type data associated with this point in time
 */
public class DiscreteTimeData<T> {

    private long millis;
    private T data;

    public DiscreteTimeData(long millis, T data) {
        this.millis = millis;
        this.data = data;
    }

    public long getTimeInMillis() {
        return millis;
    }

    public T getData() {
        return data;
    }

}
