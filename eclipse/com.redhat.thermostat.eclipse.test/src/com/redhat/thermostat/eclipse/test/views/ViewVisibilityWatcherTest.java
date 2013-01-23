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

package com.redhat.thermostat.eclipse.test.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.internal.views.ViewVisibilityWatcher;

public class ViewVisibilityWatcherTest {
    private static final Long TIME_OUT_MILLIS = 5000L;
    private static final String VIEW_ID = IPageLayout.ID_PROBLEM_VIEW;
    private IViewPart view;
    private Shell shell;
    private CountDownLatch latch;
    private Action action;
    private ViewVisibilityWatcher watcher;

    @Before
    public void beforeTest() throws Exception {
        shell = new Shell(Display.getCurrent());
        
        latch = new CountDownLatch(1);
        action = null;
        
        SWTComponent component = new SWTComponent() {
            
            @Override
            public void show() {
                action = Action.VISIBLE;
                latch.countDown();
            }
            
            @Override
            public void hide() {
                action = Action.HIDDEN;
                latch.countDown();
            }
        };
        
        watcher = new ViewVisibilityWatcher(component);
    }

    @After
    public void afterTest() throws Exception {
        shell = null;
    }

    @Test
    public void testVisibleBeforeAttach() throws Exception {
        showView();

        // Attach
        watcher.watch(shell, VIEW_ID);

        waitForAction();

        assertEquals(Action.VISIBLE, action);
    }

    @Test
    public void testVisibleAfterAttach() throws Exception {
        // Attach
        watcher.watch(shell, VIEW_ID);

        showView();

        waitForAction();

        assertEquals(Action.VISIBLE, action);
    }

    @Test
    public void testVisibleBeforeAttachHiddenAfter() throws Exception {
        showView();

        // Attach
        watcher.watch(shell, VIEW_ID);

        waitForAction();

        assertEquals(Action.VISIBLE, action);

        // Hide view
        latch = new CountDownLatch(1);
        action = null;

        hideView();

        waitForAction();

        assertEquals(Action.HIDDEN, action);
    }

    private void waitForAction()
            throws InterruptedException {
        if (!latch.await(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)) {
            fail("Timeout while waiting for action");
        }
    }

    private void showView() throws PartInitException {
        view = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().showView(VIEW_ID);
    }

    private void hideView() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .hideView(view);
    }

}

