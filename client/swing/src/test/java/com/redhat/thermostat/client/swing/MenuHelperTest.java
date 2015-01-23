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

package com.redhat.thermostat.client.swing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.shared.locale.LocalizedString;

@RunWith(CacioFESTRunner.class)
public class MenuHelperTest {

    private FrameFixture frameFixture;
    private JFrame window;
    private MenuHelper menu;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window = new JFrame();
                JMenuBar menuBar = new JMenuBar();
                window.setJMenuBar(menuBar);
                JMenu fileMenu = new JMenu("File");
                fileMenu.setName("File");
                menuBar.add(fileMenu);
                menu = new MenuHelper(menuBar);
                window.pack();
            }
        });

        frameFixture = new FrameFixture(window);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
        window = null;
    }

    @Category(GUITest.class)
    @Test
    public void addRemoveWithNewTopLevelMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("Test1");
        final LocalizedString MENU_NAME = new LocalizedString("Test2");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] { PARENT_NAME, MENU_NAME });
        when(action.getType()).thenReturn(MenuAction.Type.STANDARD);

        JMenuItemFixture menuItem;

        frameFixture.show();

        menu.addMenuAction(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        menu.removeMenuAction(action);

        try {
            menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
            // should not reach here
            assertTrue(false);
        } catch (ComponentLookupException cle) {
            // expected
        }
    }

    @Category(GUITest.class)
    @Test
    public void addRemoveToExistingMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test2");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] { PARENT_NAME, MENU_NAME });
        when(action.getType()).thenReturn(MenuAction.Type.STANDARD);

        JMenuItemFixture menuItem;

        frameFixture.show();

        assertNotNull(frameFixture.menuItem("File"));

        menu.addMenuAction(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        menu.removeMenuAction(action);

        try {
            menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
            // should not reach here
            assertTrue(false);
        } catch (ComponentLookupException cle) {
            // expected
        }
    }

    @Category(GUITest.class)
    @Test
    public void addRemoveHighlyNextedMenu() {
        final LocalizedString[] path = new LocalizedString[] {
                new LocalizedString("View"),
                new LocalizedString("Filter"),
                new LocalizedString("Virtual Machine"),
                new LocalizedString("Show Only Running")
                };
        final String[] plainPath = fromLocalizedArray(path);
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(path[path.length - 1]);
        when(action.getPath()).thenReturn(path);
        when(action.getType()).thenReturn(MenuAction.Type.STANDARD);

        JMenuItemFixture menuItem;

        frameFixture.show();

        menu.addMenuAction(action);

        menuItem = frameFixture.menuItemWithPath(plainPath);
        assertNotNull(menuItem);

        menu.removeMenuAction(action);

        try {
            menuItem = frameFixture.menuItemWithPath(plainPath);
            // should not reach here
            assertTrue(false);
        } catch (ComponentLookupException cle) {
            // expected
        }
    }

    @Category(GUITest.class)
    @Test
    public void addRadioMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] { PARENT_NAME, MENU_NAME });
        when(action.getType()).thenReturn(MenuAction.Type.RADIO);

        JMenuItemFixture menuItem;

        frameFixture.show();

        menu.addMenuAction(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        assertTrue(menuItem.target instanceof JRadioButtonMenuItem);
    }

    @Category(GUITest.class)
    @Test
    public void addCheckBoxMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getType()).thenReturn(MenuAction.Type.CHECK);
        when(action.getPath()).thenReturn(new LocalizedString[] { PARENT_NAME, MENU_NAME });

        JMenuItemFixture menuItem;

        frameFixture.show();

        menu.addMenuAction(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        assertTrue(menuItem.target instanceof JCheckBoxMenuItem);
    }

    private String[] fromLocalizedArray(LocalizedString[] localized) {
        String[] strings = new String[localized.length];
        for (int i = 0; i < localized.length; i++) {
            strings[i] = localized[i].getContents();
        }
        return strings;
    }
}

