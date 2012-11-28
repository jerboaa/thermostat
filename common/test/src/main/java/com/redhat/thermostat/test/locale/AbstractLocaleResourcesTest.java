package com.redhat.thermostat.test.locale;

import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractLocaleResourcesTest<T extends Enum<T>> {

    @Test
    public void testLocalizedStringsArePresent() throws IOException {
        
        String stringsResource = "/" + getResourceBundle().replace(".", "/") + ".properties";
        
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(stringsResource));
        
        Assert.assertEquals(values().length, props.values().size());
        for (T resource : values()) {
            Assert.assertTrue("missing property from resource bound file: " + resource,
                              props.containsKey(resource.name()));
        }
    }
    
    private T[] values() {
        return getEnumClass().getEnumConstants();
    }
    
    protected abstract Class<T> getEnumClass();
    
    protected abstract String getResourceBundle();

}