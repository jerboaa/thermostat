/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.gc.remote.client.swing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Dimension;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.gc.remote.client.common.RequestGCAction;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class ToolbarGCButtonTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private ToolbarGCButton gcButton;
        
    private RequestGCAction action;
    
    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }
    
    @Before
    public void setUp() {
        
        final Preferences prefs = mock(Preferences.class);
        
        action = mock(RequestGCAction.class);
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                
                HeaderPanel header = new HeaderPanel(prefs, new LocalizedString("Test Panel"));
                header.setName("headerPanel");
                
                JPanel content = new JPanel();
                content.setName("contentPanel");
                
                header.setContent(content);
               
                gcButton = new ToolbarGCButton(action);
                gcButton.setName("gcButton");
                header.addToolBarButton(gcButton);
                
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
    }
    
    @Test
    public void testEventDelivered() throws InterruptedException {
        frameFixture.show();
        
        JButtonFixture button = frameFixture.button("gcButton");
        button.requireVisible();
        
        button.click();
        
        // timing dependent test...
        Thread.sleep(250);
        
        verify(action).requestGC();
    }
}

