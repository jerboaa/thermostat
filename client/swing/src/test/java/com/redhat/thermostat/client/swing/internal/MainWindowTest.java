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

package com.redhat.thermostat.client.swing.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.redhat.thermostat.client.core.internal.platform.EmbeddedPlatformService;
import com.redhat.thermostat.client.ui.ContentProvider;
import com.redhat.thermostat.shared.config.CommonPaths;
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
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import org.osgi.framework.BundleContext;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.File;
import java.util.Dictionary;
import java.util.logging.Logger;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class MainWindowTest {

    private FrameFixture frameFixture;
    private MainWindow window;
    private ActionListener<MainView.Action> l;
    private BundleContext context;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings("unchecked") // mock(ActionListener.class)
    @Before
    public void setUp() {

        context = mock(BundleContext.class);

        GuiActionRunner.execute(new GuiTask() {
            
            @Override
            protected void executeInEDT() throws Throwable {
                window = new MainWindow() {
                    @Override
                    BundleContext getContext() {
                        return context;
                    }
                };
                l = mock(ActionListener.class);
                window.addActionListener(l);
                CommonPaths commonPaths = mock(CommonPaths.class);
                File sharedPrefs = File.createTempFile("thermostat-mainwindowtest", null);
                when(commonPaths.getUserSharedPreferencesFile()).thenReturn(sharedPrefs);
                window.setCommonPaths(commonPaths);
            }
        });

        frameFixture = new FrameFixture(window);
    }
    
    @Category(GUITest.class)
    @Test
    public void assertPlatformServiceRegistered() {
        verify(context).registerService(eq(EmbeddedPlatformService.class),
                                        any(EmbeddedPlatformService.class),
                                        eq((Dictionary<String, ?>) null));
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
        window = null;
        l = null;
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatCloseFiresShutdownEvent() {

        frameFixture.show();

        frameFixture.close();
        frameFixture.requireNotVisible();
        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHUTDOWN));
    }

    @Category(GUITest.class)
    @Test
    public void verifyShowMainWindowShowsWindow() {
        window.showMainWindow();
        frameFixture.requireVisible();
    }

    @Category(GUITest.class)
    @Test
    public void verifyHideMainWindowHidesWindow() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window.showMainWindow();
            }
        });
        frameFixture.requireVisible();
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window.hideMainWindow();
            }
        });
        frameFixture.requireNotVisible();
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatClientPreferencesMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showClientConfig");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_CLIENT_CONFIG));
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatAgentPreferencesMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showAgentConfig");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_AGENT_CONFIG));
    }

    @Category(GUITest.class)
    @Test
    public void verifyUserGuideMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showUserGuide");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_USER_GUIDE));
    }

    @Category(GUITest.class)
    @Test
    public void addRemoveMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test2");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});
        when(action.getType()).thenReturn(MenuAction.Type.STANDARD);

        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);
        menuItem.click();

        verify(action).execute();

        window.removeMenu(action);

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
    public void addRadioMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});


        when(action.getType()).thenReturn(MenuAction.Type.RADIO);

        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

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
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});


        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        assertTrue(menuItem.target instanceof JCheckBoxMenuItem);
    }

    @GUITest
    @Test
    public void verifyThatAddingNonSwingSubViewFails() throws AWTException {

        Logger logger = mock(Logger.class);
        window.__test__setLogger(logger);

        BasicView subView = mock(BasicView.class);
        ContentProvider subViewProvider = mock(ContentProvider.class);
        when(subViewProvider.getView()).thenReturn(subView);

        frameFixture.show();
        window.setContent(subViewProvider);

        Robot robot = new Robot();
        robot.waitForIdle();

        verify(logger).severe(anyString());
    }
}

