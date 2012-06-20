package com.redhat.thermostat.client.internal.osgi;

import static org.junit.Assert.*;

import org.junit.Test;

public class ApplicationServiceProviderTest {

    @Test
    public void testCache() {
        ApplicationServiceProvider provider = new ApplicationServiceProvider();
        provider.getApplicationCache().addAttribute("test", "fluff");
        assertEquals("fluff", provider.getApplicationCache().getAttribute("test"));
    }

}
