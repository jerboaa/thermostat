package com.redhat.thermostat.common;

public class NotImplementedException extends RuntimeException {

    private static final long serialVersionUID = -1620198443624195618L;

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

}
