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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.swing.UIManager;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.matcher.JButtonMatcher;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JTextComponentFixture;
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
public class ClientConfigurationSwingTest {
    
    private ClientConfigurationSwing frame;
    private DialogFixture frameFixture;
    private ActionListener<ClientConfigurationView.Action> l;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings("unchecked") // ActionListener
    @Before
    public void setUp() {
        
        frame = GuiActionRunner.execute(new GuiQuery<ClientConfigurationSwing>() {

            @Override
            protected ClientConfigurationSwing executeInEDT() throws Throwable {
                return new ClientConfigurationSwing();
            }
        });
        l = mock(ActionListener.class);
        frame.addListener(l);
        frame.showDialog();
        assertNotNull(frame.getDialog());
        frameFixture = new DialogFixture(frame.getDialog());

    }

    @After
    public void tearDown() {
        frame.hideDialog();

        frameFixture.cleanUp();
        frame.removeListener(l);
        frame = null;
        l = null;
    }

    @Category(GUITest.class)
    @Test
    public void testConnectionUrlText() {

        JTextComponentFixture textBox = frameFixture.textBox("connectionUrl");
        textBox.enterText("foobar");

        assertEquals("foobar", frame.getConnectionUrl());
    }

    @Category(GUITest.class)
    @Test
    public void testPasswordText() {

        JTextComponentFixture textBox = frameFixture.textBox("password");
        textBox.enterText("foobar");

        assertEquals("foobar", frame.getPassword());
    }
    
    @Category(GUITest.class)
    @Test
    public void testUsernameText() {

        JTextComponentFixture textBox = frameFixture.textBox("username");
        textBox.enterText("foobar");

        assertEquals("foobar", frame.getUserName());
    }
    
    @Category(GUITest.class)
    @Test
    public void testOkayButton() {
        JButtonFixture button = frameFixture.button(JButtonMatcher.withText("OK"));
        button.click();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_ACCEPT)));
    }

    @Category(GUITest.class)
    @Test
    public void testCancelButton() {
        JButtonFixture button = frameFixture.button(JButtonMatcher.withText(UIManager.getString("OptionPane.cancelButtonText")));
        button.click();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_CANCEL)));
    }

    @Category(GUITest.class)
    @Test
    public void testCloseWindow() {
        frameFixture.close();

        verify(l).actionPerformed(eq(new ActionEvent<>(frame, ClientConfigurationView.Action.CLOSE_CANCEL)));
    }

    @Bug(id="1030",
         summary="Buttons in client preferences dialog should have the same size",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1030")
    @Category(GUITest.class)
    @Test
    public void testButtonsSameSize() {
        JButtonFixture cancel = frameFixture.button(JButtonMatcher.withText(UIManager.getString("OptionPane.cancelButtonText")));
        JButtonFixture ok = frameFixture.button(JButtonMatcher.withText("OK"));

        assertEquals(cancel.target.getSize(), ok.target.getSize());

        frameFixture.close();
    }
}
