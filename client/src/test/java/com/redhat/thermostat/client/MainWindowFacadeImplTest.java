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

package com.redhat.thermostat.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.mongodb.DB;
import com.redhat.thermostat.client.ui.MainWindow;

public class MainWindowFacadeImplTest {

    private PropertyChangeListener l;

    private MainWindowFacadeImpl controller;

    private MainWindow view;

    @Before
    public void setUp() {
        
        DB db = mock(DB.class);
        controller = new MainWindowFacadeImpl(db);
        controller = spy(controller);
        view = mock(MainWindow.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                l = (PropertyChangeListener) invocation.getArguments()[0];
                return null;
            }
            
        }).when(view).addViewPropertyListener(any(PropertyChangeListener.class));
        controller.initView(view);
    }

    @After
    public void tearDown() {
        view = null;
        controller = null;
        l = null;
    }

    @Test
    public void verifyThatShutdownEventStopsController() {

        l.propertyChange(new PropertyChangeEvent(view, MainWindow.SHUTDOWN_PROPERTY, false, true));

        verify(controller).stop();

    }

    @Test
    public void verifyThatHostsVmsFilterChangeUpdatesTree() {

        l.propertyChange(new PropertyChangeEvent(view, MainWindow.HOST_VM_TREE_FILTER_PROPERTY, null, "test"));

        verify(view).updateTree(eq("test"), any(HostsVMsLoader.class));

    }
}
