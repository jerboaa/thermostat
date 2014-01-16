/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import java.awt.Color;
import java.awt.Font;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicLookAndFeel;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.test.Bug;

@Bug(id="976",
     summary="About dialog creashes with GTK look and feel",
     url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=976")
public class UIResourcesTest {
   
    private static LookAndFeel originalLaf;
    
    @BeforeClass
    public static void setUp() throws Exception {
        final Object [] uiDefaults = new Object[] {
            "Button.darkShadow", null,
            "Button.focus", null,
            "Label.font", null
        };
        
        originalLaf = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new BasicLookAndFeel() {
            
            @Override
            protected void initClassDefaults(UIDefaults table) {
                super.initClassDefaults(table);
                table.putDefaults(uiDefaults);
            }
            
            @Override
            protected void initSystemColorDefaults(UIDefaults table) {
                super.initSystemColorDefaults(table);
                table.putDefaults(uiDefaults);
            }
            
            @Override
            protected void initComponentDefaults(UIDefaults table) {
                super.initComponentDefaults(table);
                table.putDefaults(uiDefaults);
            }
            
            @Override
            public boolean isSupportedLookAndFeel() {
                return true;
            }
            
            @Override
            public boolean isNativeLookAndFeel() {
                return false;
            }
            
            @Override
            public String getName() {
                return "fluff";
            }
            
            @Override
            public String getID() {
                return "fluff";
            }
            
            @Override
            public String getDescription() {
                return "fluff";
            }
        });
    }
    
    @Test
    public void testHyperlinkColor() {
        Assert.assertEquals(Color.BLUE, UIResources.getInstance().hyperlinkColor());
    }

    @Test
    public void testHyperlinkActiveColor() {
        Assert.assertEquals(Color.BLUE, UIResources.getInstance().hyperlinkActiveColor());
    }

    @Test
    public void testStandardFont() {
        Assert.assertEquals(Font.DIALOG, UIResources.getInstance().standardFont().getName());
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (originalLaf != null) {
            UIManager.setLookAndFeel(originalLaf);
        } else {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
    }
}

