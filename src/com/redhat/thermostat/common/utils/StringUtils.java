package com.redhat.thermostat.common.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringUtils {

    private StringUtils() {
        /* should not be instantiated */
    }

    public static InputStream toInputStream(String toConvert) {
        try {
            return new ByteArrayInputStream(toConvert.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported");
        }
    }
}
