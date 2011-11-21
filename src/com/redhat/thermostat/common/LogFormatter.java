package com.redhat.thermostat.common;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getLevel());
        sb.append(": ");
        sb.append(record.getMessage());
        sb.append("\n");
        return sb.toString();
    }
}
