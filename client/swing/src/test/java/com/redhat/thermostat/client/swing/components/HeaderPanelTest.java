/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.Dimension;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JPanelFixture;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.shared.locale.LocalizedString;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class HeaderPanelTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private HeaderPanel header;
    
    private Icon someIcon1;
    private Icon someIcon2;
    private Icon someIcon3;

    private Preferences prefs;
    
    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }
    
    @Before
    public void setUp() {
        Random random = new Random(); 
        prefs = Preferences.userRoot().node(HeaderPanelTest.class.getName() + "." + random.nextInt());
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                
                header = new HeaderPanel(prefs, new LocalizedString("Test Panel"));
                header.setName("headerPanel");
                
                JPanel content = new JPanel();
                content.setName("contentPanel");
                
                header.setContent(content);
                
                someIcon1 = new EmptyIcon(16, 16);
                someIcon2 = new EmptyIcon(16, 16);
                someIcon3 = new EmptyIcon(16, 16);
                
                ActionButton button1 = new ActionButton(someIcon1, new LocalizedString("button1"));
                button1.setName("button1");
                
                ActionButton button2 = new ActionButton(someIcon2, new LocalizedString("button2"));
                button2.setName("button2");
                
                ActionToggleButton toggle1 = new ActionToggleButton(someIcon3, new LocalizedString("toggle1"));
                toggle1.setName("toggle1");

                header.addToolBarButton(button1);
                header.addToolBarButton(button2);
                header.addToolBarButton(toggle1);
                
                frame.getContentPane().add(header);
                
                frame.setMinimumSize(new Dimension(800, 300));
            }
        });
        frameFixture = new FrameFixture(frame);
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        frameFixture.cleanUp();
        frameFixture = null;
        prefs.removeNode();
    }
    
    @Test
    public void testContentPaneAdded() {
        frameFixture.show();
        
        JPanelFixture contentPanel = frameFixture.panel("contentPanel");
        contentPanel.requireVisible();
    }
    
    @Test
    public void testContentAdded() {
        frameFixture.show();
        final LocalizedString[] results = new LocalizedString[2];
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                results[0] = header.getHeader();
                header.setHeader(new LocalizedString("fluff"));
                results[1] = header.getHeader();
            }
        });
        
        assertEquals("Test Panel", results[0].getContents());
        assertEquals("fluff", results[1].getContents());
        
        // do it again, with a new header
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                
                HeaderPanel header = new HeaderPanel(new LocalizedString("Test"));
                
                results[0] = header.getHeader();
                header.setHeader(new LocalizedString("fluff"));
                results[1] = header.getHeader();
            }
        });
        
        assertEquals("Test", results[0].getContents());
        assertEquals("fluff", results[1].getContents());
    }
    
    @Test
    public void testHeaderHasText() throws InterruptedException {
        frameFixture.show();

        JPanelFixture contentPanel = frameFixture.panel("contentPanel");
        contentPanel.requireVisible();

        frameFixture.button("button1").requireVisible();
        frameFixture.button("button2").requireVisible();
        frameFixture.toggleButton("toggle1").requireVisible();
    }
    
    @Test
    public void testShowToolbarText() throws InterruptedException {
        frameFixture.show();
        
        JPanelFixture headerPanel = frameFixture.panel("clickableArea");
        headerPanel.requireVisible();
        
        assertFalse(header.isShowToolbarText());

        frameFixture.button("button1").requireText("");

        headerPanel.showPopupMenu().click();

        assertTrue(header.isShowToolbarText());

        frameFixture.button("button1").requireVisible();
        frameFixture.button("button2").requireVisible();
        frameFixture.toggleButton("toggle1").requireVisible();
        
        frameFixture.button("button1").requireText("button1");
        
        headerPanel.showPopupMenu().click();
        
        assertFalse(header.isShowToolbarText());

        frameFixture.button("button1").requireText("");

        frameFixture.button("button1").requireVisible();
        frameFixture.button("button2").requireVisible();
        frameFixture.toggleButton("toggle1").requireVisible();
    }
}

