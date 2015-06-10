/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.models;

import static org.junit.Assert.*;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class NullSelectionModelTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private JList<String> list;
    
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
                
                DefaultListModel<String> model = new DefaultListModel<>();
                
                model.addElement("fluff #1");
                model.addElement("fluff #2");
                model.addElement("fluff #3");
                model.addElement("fluff #4");
                model.addElement("fluff #5");

                list = new JList<>(model);
                list.setSelectionModel(new NullSelectionModel());
                
                frame.add(list);
            }
        });
        frameFixture = new FrameFixture(frame);
    }
    
    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }
    
    @GUITest
    @Test
    public void testCellNotSelectable() {
        frameFixture.show();
        
        String[] selection = frameFixture.list().selection();
        assertEquals(0, selection.length);
        frameFixture.list().clickItem(2);
        selection = frameFixture.list().selection();
        assertEquals(0, selection.length);
    }

}

