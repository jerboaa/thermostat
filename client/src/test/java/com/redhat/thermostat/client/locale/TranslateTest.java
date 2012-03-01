package com.redhat.thermostat.client.locale;

import java.util.Locale;
import java.util.MissingResourceException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.redhat.thermostat.client.Translate.localize;

public class TranslateTest {

    private Locale lang;
    
    @Before
    public void setUp() {
        this.lang = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }
    
    @After
    public void tearDown() {
        Locale.setDefault(lang);
    }
    
    @Test
    public void testMissingInfo() {
        String testString = localize("MISSING_INFO");
        Assert.assertEquals("Missing Information", testString);
    }
    
    @Test
    public void testLocalization() {
        String testString = localize("APPLICATION_INFO_VERSION", "test");
        Assert.assertEquals("Version test", testString);
        
        testString = localize("APPLICATION_INFO_DESCRIPTION");
        Assert.assertEquals("A monitoring and serviceability tool for OpenJDK",
                            testString);
    }
    
    @Test(expected = MissingResourceException.class)
    public void testLocalizationError1() {
        
        localize("INVALID_BLABLABLA_FLUFF");
        Assert.fail("java.util.MissingResourceException expected");
    }
    
    @Test(expected = MissingResourceException.class)
    public void testLocalizationError2() {
        
        localize("INVALID_BLABLABLA_FLUFF", "test");
        Assert.fail("java.util.MissingResourceException expected");
    }
}
