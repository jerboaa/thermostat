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

package com.redhat.thermostat.client.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JFrame;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.locale.LocalizedString;

@RunWith(CacioFESTRunner.class)
public class ActionButtonTest {

    private FrameFixture frameFixture;
    private Preferences prefs;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    
    @Before
    public void setUp() {
        Random random = new Random(); 
        prefs = Preferences.userRoot().node(HeaderPanelTest.class.getName() + "." + random.nextInt());
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        if (frameFixture != null) {
            frameFixture.cleanUp();
        }
        prefs.removeNode();
    }

    @Test
    public void testActionButton() {
        JFrame frame = GuiActionRunner.execute(new GuiQuery<JFrame>() {
            @Override
            protected JFrame executeInEDT() throws Throwable {

                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                HeaderPanel header = new HeaderPanel(prefs, "wrong");
                header.setHeader("Test");

                Icon icon = new Icon() {

                    @Override
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.setColor(Color.CYAN);
                        g.fillRect(x, y, 16, 16);

                    }

                    @Override
                    public int getIconWidth() {
                        // TODO Auto-generated method stub
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        // TODO Auto-generated method stub
                        return 16;
                    }
                };

                ActionButton button = new ActionButton(icon, new LocalizedString("Fluff"));
                button.setName("button");
                header.addToolBarButton(button);

                frame.getContentPane().add(header);
                frame.setSize(500, 500);
                frame.setVisible(true);
                return frame;
            }
        });
        frameFixture = new FrameFixture(frame);
        JButtonFixture actionButton = frameFixture.button("button");
        actionButton.requireText("");
        actionButton.requireEnabled();

    }

}

