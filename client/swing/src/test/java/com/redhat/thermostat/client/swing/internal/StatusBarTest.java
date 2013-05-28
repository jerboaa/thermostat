/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.*;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Semaphore;

import javax.swing.JFrame;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.shared.locale.LocalizedString;

@RunWith(CacioFESTRunner.class)
public class StatusBarTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    
    private StatusBar statusBar;
    
    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }
    
    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                frame.getContentPane().setLayout(new BorderLayout());
                
                statusBar = new StatusBar();
                frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
                
                frame.setSize(500, 500);
            }
        });
        frameFixture = new FrameFixture(frame);
    }
    
    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }
    
    @Test
    @GUITest
    @RunsInEDT
    public void testSetPrimaryStatusLabel() throws InterruptedException {
        frameFixture.show();
        
        JLabelFixture labelfixture = frameFixture.label("primaryStatusLabel");
        labelfixture.requireText("");
        
        final Semaphore sem = new Semaphore(0);
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                statusBar.setPrimaryStatus(new LocalizedString("test"));
                sem.release();
            }
        });
        sem.acquire();
        
        // the label has an extra space at the beginning
        labelfixture.requireText(" test");
    }
    
    @Test
    @GUITest
    @RunsInEDT
    public void testSetPrimaryStatusLabelWithProperty() throws InterruptedException {
        frameFixture.show();
        
        final String[] primaryStatus = new String[2];
        
        final Semaphore sem = new Semaphore(2);
        statusBar.addPropertyChangeListener(StatusBar.PRIMARY_STATUS_PROPERTY,
                                                      new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                primaryStatus[0] = (String) evt.getOldValue();
                primaryStatus[1] = (String) evt.getNewValue();
                sem.release();
            }
        });
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                statusBar.setPrimaryStatus(new LocalizedString("test"));
                sem.release();
            }
        });
        sem.acquire(2);
        
        assertEquals("", primaryStatus[0]);
        assertEquals("test", primaryStatus[1]);
    }
}

