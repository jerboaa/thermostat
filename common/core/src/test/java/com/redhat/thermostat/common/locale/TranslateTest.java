/*
 * Copyright 2012 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.common.locale;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void testLocalizeWithoutArguments() {
        String testString = LocaleResources.createLocalizer().localize(LocaleResources.MISSING_INFO);
        Assert.assertEquals("Missing Information", testString);
    }
    
    @Test
    public void testLocalizeWithArguments() {
        Translate t = LocaleResources.createLocalizer();
        
        String testString = t.localize(LocaleResources.APPLICATION_INFO_DESCRIPTION);
        Assert.assertEquals("A monitoring and serviceability tool for OpenJDK",
                            testString);
        testString = t.localize(LocaleResources.APPLICATION_INFO_LICENSE);
        Assert.assertEquals("Licensed under GPLv2+ with Classpath exception",
                testString);
    }
    
    @Test
    public void testLocalizedStringsArePresent() throws IOException {
        
        String stringsResource = "/" + LocaleResources.RESOURCE_BUNDLE.replace(".", "/") + ".properties";
        
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(stringsResource));
        
        Assert.assertEquals(LocaleResources.values().length, props.values().size());
        for (LocaleResources resource : LocaleResources.values()) {
            Assert.assertTrue("missing property from resource bound file: " + resource,
                              props.containsKey(resource.name()));
        }
    }
}
