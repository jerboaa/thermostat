package com.redhat.thermostat.client.osgi.service;

import static org.junit.Assert.*;

import org.junit.Test;

public class ApplicationCacheTest {

    @Test
    public void verityCache() {
        ApplicationCache cache = new ApplicationCache();
        cache.addAttribute("test", "fluff");
        assertEquals("fluff", cache.getAttribute("test"));
    }
}
