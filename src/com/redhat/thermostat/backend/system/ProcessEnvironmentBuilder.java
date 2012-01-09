package com.redhat.thermostat.backend.system;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

public class ProcessEnvironmentBuilder {

    private static final Logger logger = LoggingUtils.getLogger(ProcessEnvironmentBuilder.class);

    private ProcessEnvironmentBuilder() {
        /* should not be instantiated */
    }

    public static Map<String, String> build(int pid) {
        Map<String, String> env = new HashMap<String, String>();

        String filename = "/proc/" + pid + "/environ";
        try {
            Reader reader = new FileReader(filename);
            try {
                char[] fileBuffer = new char[1024];
                int fileBufferIndex = 0;
                char[] buffer = new char[1024];
                int read = 0;
                while (true) {
                    read = reader.read(buffer);
                    if (read == -1) {
                        break;
                    }

                    if (read + fileBufferIndex > fileBuffer.length) {
                        char[] newFileBuffer = new char[fileBuffer.length * 2];
                        System.arraycopy(fileBuffer, 0, newFileBuffer, 0, fileBufferIndex);
                        fileBuffer = newFileBuffer;
                    }
                    System.arraycopy(buffer, 0, fileBuffer, fileBufferIndex, read);
                    fileBufferIndex = fileBufferIndex + read;

                }
                List<String> parts = getParts(fileBuffer, fileBufferIndex);
                for (String part : parts) {
                    int splitterPos = part.indexOf("=");
                    String key = part.substring(0, splitterPos);
                    String value = part.substring(splitterPos + 1);
                    env.put(key, value);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "error reading " + filename, e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.log(Level.WARNING, "error closing " + filename);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "file " + filename + " not found");
        }

        return env;
    }

    /**
     * Split a char array, where items are separated by a null into into a list
     * of strings
     */
    private static List<String> getParts(char[] nullSeparatedBuffer, int bufferLength) {
        int maxLength = Math.min(nullSeparatedBuffer.length, bufferLength);
        List<String> parts = new ArrayList<String>();

        int lastStart = 0;
        for (int i = 0; i < maxLength; i++) {
            if (nullSeparatedBuffer[i] == '\0') {
                String string = new String(nullSeparatedBuffer, lastStart, (i - lastStart));
                parts.add(string);
                lastStart = i + 1;
            }
        }
        return parts;
    }

}
