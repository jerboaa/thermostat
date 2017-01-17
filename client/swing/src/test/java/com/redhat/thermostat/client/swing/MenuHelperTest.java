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

package com.redhat.thermostat.client.swing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

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

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.shared.locale.LocalizedString;

import java.util.HashMap;
import java.util.Map;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class MenuHelperTest {

    private MenuStates menuStates;
    private FrameFixture frameFixture;
    private JFrame window;
    private MenuHelper menu;
    private JMenuBar menuBar;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        menuStates = mock(MenuStates.class);
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window = new JFrame();
                menuBar = new JMenuBar();
                window.setJMenuBar(menuBar);
                JMenu fileMenu = new JMenu("File");
                fileMenu.setName("File");
                menuBar.add(fileMenu);
                menu = new MenuHelper(menuStates, menuBar);
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

    @Category(GUITest.class)
    @Test
    public void addSeparatorToDynamicallyPopulatedMenus() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getType()).thenReturn(MenuAction.Type.CHECK);
        when(action.getPath()).thenReturn(new LocalizedString[]{PARENT_NAME, MENU_NAME});

        assertThat(window.getJMenuBar().getMenu(0).getMenuComponentCount(), is((0)));

        frameFixture.show();

        menu.addMenuAction(action);

        assertThat(window.getJMenuBar().getMenu(0).getMenuComponentCount(), is(2));
        assertThat(window.getJMenuBar().getMenu(0).getMenuComponent(0), instanceOf(JSeparator.class));
    }

    @Category(GUITest.class)
    @Test
    public void menuActionsAreSorted() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");

        final LocalizedString MENU_A = new LocalizedString("MenuA");
        MenuAction actionA = mock(MenuAction.class);
        when(actionA.getName()).thenReturn(MENU_A);
        when(actionA.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionA.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_A});
        when(actionA.sortOrder()).thenReturn(MenuAction.SORT_BOTTOM);

        final LocalizedString MENU_B = new LocalizedString("MenuB");
        MenuAction actionB = mock(MenuAction.class);
        when(actionB.getName()).thenReturn(MENU_B);
        when(actionB.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionB.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_B});
        when(actionB.sortOrder()).thenReturn(MenuAction.SORT_TOP + 10);

        final LocalizedString MENU_C = new LocalizedString("MenuC");
        MenuAction actionC = mock(MenuAction.class);
        when(actionC.getName()).thenReturn(MENU_C);
        when(actionC.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionC.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_C});
        when(actionC.sortOrder()).thenReturn(MenuAction.SORT_TOP);

        frameFixture.show();

        menu.addMenuAction(actionA);
        menu.addMenuAction(actionB);
        menu.addMenuAction(actionC);

        assertThat(window.getJMenuBar().getMenu(0).getMenuComponentCount(), is(4));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(0).getMenuComponent(1)).getText(), is(MENU_C.getContents()));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(0).getMenuComponent(2)).getText(), is(MENU_B.getContents()));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(0).getMenuComponent(3)).getText(), is(MENU_A.getContents()));
    }

    @Category(GUITest.class)
    @Test
    public void menuActionSortingWorksCorrectlyWithMultipleMenus() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                JMenu editMenu = new JMenu("Edit");
                editMenu.setName("Edit");
                menuBar.add(editMenu);
            }
        });

        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString PARENT2_NAME = new LocalizedString("Edit");

        final LocalizedString MENU_A = new LocalizedString("MenuA");
        MenuAction actionA = mock(MenuAction.class);
        when(actionA.getName()).thenReturn(MENU_A);
        when(actionA.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionA.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_A});
        when(actionA.sortOrder()).thenReturn(MenuAction.SORT_BOTTOM);

        final LocalizedString MENU_B = new LocalizedString("MenuB");
        MenuAction actionB = mock(MenuAction.class);
        when(actionB.getName()).thenReturn(MENU_B);
        when(actionB.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionB.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_B});
        when(actionB.sortOrder()).thenReturn(MenuAction.SORT_TOP + 20);

        final LocalizedString MENU_C = new LocalizedString("MenuC");
        MenuAction actionC = mock(MenuAction.class);
        when(actionC.getName()).thenReturn(MENU_C);
        when(actionC.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionC.getPath()).thenReturn(new LocalizedString[] {PARENT2_NAME, MENU_C});
        when(actionC.sortOrder()).thenReturn(MenuAction.SORT_TOP + 10);

        final LocalizedString MENU_D = new LocalizedString("MenuD");
        MenuAction actionD = mock(MenuAction.class);
        when(actionD.getName()).thenReturn(MENU_D);
        when(actionD.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionD.getPath()).thenReturn(new LocalizedString[] {PARENT2_NAME, MENU_D});
        when(actionD.sortOrder()).thenReturn(MenuAction.SORT_TOP);

        frameFixture.show();

        menu.addMenuAction(actionA);
        menu.addMenuAction(actionB);
        menu.addMenuAction(actionC);
        menu.addMenuAction(actionD);

        assertThat(window.getJMenuBar().getMenu(0).getMenuComponentCount(), is(3));
        assertThat(window.getJMenuBar().getMenu(1).getMenuComponentCount(), is(3));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(0).getMenuComponent(1)).getText(), is(MENU_B.getContents()));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(0).getMenuComponent(2)).getText(), is(MENU_A.getContents()));

        assertThat(((JMenuItem) window.getJMenuBar().getMenu(1).getMenuComponent(1)).getText(), is(MENU_D.getContents()));
        assertThat(((JMenuItem) window.getJMenuBar().getMenu(1).getMenuComponent(2)).getText(), is(MENU_C.getContents()));
    }

    @Category(GUITest.class)
    @Test
    public void testMenuActionsStatesAreSaved() {
        LocalizedString parentName = new LocalizedString("File");
        LocalizedString menuAName = new LocalizedString("Menu A");
        LocalizedString menuBName = new LocalizedString("Menu B");

        String menuAKey = "menu-a-key";
        MenuAction actionA = mock(MenuAction.class);
        when(actionA.getName()).thenReturn(menuAName);
        when(actionA.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionA.getPath()).thenReturn(new LocalizedString[] { parentName, menuAName });
        when(actionA.sortOrder()).thenReturn(MenuAction.SORT_TOP);
        when(actionA.getPersistenceID()).thenReturn(menuAKey);

        String menuBKey = "menu-b-key";
        MenuAction actionB = mock(MenuAction.class);
        when(actionB.getName()).thenReturn(menuBName);
        when(actionB.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionB.getPath()).thenReturn(new LocalizedString[] { parentName, menuBName });
        when(actionB.sortOrder()).thenReturn(MenuAction.SORT_TOP + 1);
        when(actionB.getPersistenceID()).thenReturn(menuBKey);

        frameFixture.show();

        menu.addMenuAction(actionA);
        menu.addMenuAction(actionB);

        JMenuItemFixture menuA = frameFixture.menuItemWithPath(parentName.getContents(), menuAName.getContents());

        menuA.click();

        menu.saveMenuStates();

        Map<String, Boolean> expected = new HashMap<>();
        expected.put(menuAKey, true);
        expected.put(menuBKey, false);

        verify(menuStates).setMenuStates(expected);
    }


    @Category(GUITest.class)
    @Test
    public void testMenuActionsStatesAreRestored() {
        LocalizedString parentName = new LocalizedString("File");
        LocalizedString menuAName = new LocalizedString("Menu A");
        LocalizedString menuBName = new LocalizedString("Menu B");

        String menuAKey = "menu-a-key";
        MenuAction actionA = mock(MenuAction.class);
        when(actionA.getName()).thenReturn(menuAName);
        when(actionA.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionA.getPath()).thenReturn(new LocalizedString[] { parentName, menuAName });
        when(actionA.sortOrder()).thenReturn(MenuAction.SORT_TOP);
        when(actionA.getPersistenceID()).thenReturn(menuAKey);
        when(menuStates.getMenuState(menuAKey)).thenReturn(true);

        String menuBKey = "menu-b-key";
        MenuAction actionB = mock(MenuAction.class);
        when(actionB.getName()).thenReturn(menuBName);
        when(actionB.getType()).thenReturn(MenuAction.Type.CHECK);
        when(actionB.getPath()).thenReturn(new LocalizedString[] { parentName, menuBName });
        when(actionB.sortOrder()).thenReturn(MenuAction.SORT_TOP + 1);
        when(actionB.getPersistenceID()).thenReturn(menuBKey);
        when(menuStates.getMenuState(menuBKey)).thenReturn(false);

        frameFixture.show();

        menu.addMenuAction(actionA);
        menu.addMenuAction(actionB);

        JMenuItemFixture menuA = frameFixture.menuItemWithPath(parentName.getContents(), menuAName.getContents());
        JMenuItemFixture menuB = frameFixture.menuItemWithPath(parentName.getContents(), menuBName.getContents());

        verify(menuStates).getMenuState(menuAKey);
        verify(menuStates).getMenuState(menuBKey);

        assertThat(menuA.component().isSelected(), is(true));
        assertThat(menuB.component().isSelected(), is(false));

        verify(actionA).execute();
        verify(actionB, never()).execute();
    }

    private String[] fromLocalizedArray(LocalizedString[] localized) {
        String[] strings = new String[localized.length];
        for (int i = 0; i < localized.length; i++) {
            strings[i] = localized[i].getContents();
        }
        return strings;
    }
}

