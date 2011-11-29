package com.redhat.thermostat.common;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public synchronized String format(LogRecord record) {
        String level = record.getLevel().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(level);
        sb.append(": ");
        sb.append(record.getMessage());
        sb.append("\n");
        Throwable thrown = record.getThrown();
        String indent = "  ";
        while (thrown != null) {
            sb.append(level);
            sb.append(":");
            sb.append(indent);
            sb.append("Caused by:\n");
            sb.append(level);
            sb.append(":  ");
            sb.append(indent);
            sb.append(thrown.getClass().getCanonicalName());
            sb.append(": ");
            sb.append(thrown.getMessage());
            sb.append("\n");
            StackTraceElement[] stack = thrown.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                sb.append(level);
                sb.append(":    ");
                sb.append(indent);
                sb.append(stack[i].toString());
                sb.append("\n");
            }
            thrown = thrown.getCause();
            indent = indent.concat("  ");
        }
        return sb.toString();
    }
}
