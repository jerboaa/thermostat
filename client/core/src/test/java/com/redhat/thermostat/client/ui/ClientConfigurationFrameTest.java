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

package com.redhat.thermostat.client.ui;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import junit.framework.Assert;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.test.Bug;

@RunWith(CacioFESTRunner.class)
public class ClientConfigurationFrameTest {

    private ClientConfigurationFrame frame;
    private FrameFixture frameFixture;
    private ActionListener<ClientConfigurationView.Action> l;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings("unchecked") // ActionListener
    @Before
    public void setUp() {
        frame = GuiActionRunner.execute(new GuiQuery<ClientConfigurationFrame>() {

            @Override
            protected ClientConfigurationFrame executeInEDT() throws Throwable {
                 return new ClientConfigurationFrame();
            }
        });
        l = mock(ActionListener.class);
        frame.addListener(l);
        frameFixture = new FrameFixture(frame);

    }

    @After
    public void tearDown() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame.hideDialog();
            }
        });

        frameFixture.cleanUp();
        frame.removeListener(l);
        frame = null;
        l = null;
    }

    @Category(GUITest.class)
    @Test
    public void testOkayButton() {
        frameFixture.show();

        JButtonFixture button = frameFixture.button("ok");
        button.click();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_ACCEPT)));


    }

    @Category(GUITest.class)
    @Test
    public void testCancelButton() {
        frameFixture.show();

        JButtonFixture button = frameFixture.button("cancel");
        button.click();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_CANCEL)));

    }

    @Category(GUITest.class)
    @Test
    public void testCloseWindow() {
        frameFixture.show();

        frameFixture.close();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_CANCEL)));
    }
    
    @Bug(id="1030",
         summary="Buttons in client preferences dialog should have the same size",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1030")
    @Category(GUITest.class)
    @Test
    public void testButtonsSameSize() {
        frameFixture.show();
        
        JButtonFixture cancel = frameFixture.button("cancel");
        JButtonFixture ok = frameFixture.button("ok");
        
        Assert.assertEquals(cancel.target.getSize(), ok.target.getSize());
        
        frameFixture.close();
    }
}
