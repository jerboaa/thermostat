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

package com.redhat.thermostat.client.swing.internal;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.ui.MainWindowController;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;

public class MainTest {

    private ExecutorService executorService;
    private OSGIUtils serviceProvider;

    private MainWindowController mainWindowController;
    private UiFacadeFactory uiFactory;

    private Connection connection;
    private ArgumentCaptor<ConnectionListener> connectionListenerCaptor;

    private DAOFactory daoFactory;

    private TimerFactory timerFactory;

    @Before
    public void setUp() {
        ApplicationService appService = mock(ApplicationService.class);

        executorService = mock(ExecutorService.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(executorService).execute(isA(Runnable.class));

        when(appService.getApplicationExecutor()).thenReturn(executorService);

        serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getService(ApplicationService.class)).thenReturn(appService);

        mainWindowController = mock(MainWindowController.class);

        uiFactory = mock(UiFacadeFactory.class);
        when(uiFactory.getMainWindow()).thenReturn(mainWindowController);

        connection = mock(Connection.class);
        connectionListenerCaptor = ArgumentCaptor.forClass(ConnectionListener.class);
        doNothing().when(connection).addListener(connectionListenerCaptor.capture());

        daoFactory = mock(DAOFactory.class);
        when(daoFactory.getConnection()).thenReturn(connection);

        timerFactory = mock(TimerFactory.class);
    }

    /**
     * Handle all outstanding EDT events by posting a no-op event and waiting
     * until it completes.
     */
    private void handleAllEdtEvents() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                /* NO-OP */
            }
        });
    }

    @Test
    public void verifyRunWaitsForShutdown() throws Exception {
        Main main = new Main(serviceProvider, uiFactory, daoFactory, timerFactory);

        main.run();

        handleAllEdtEvents();

        verify(uiFactory).awaitShutdown();
    }

    @Test
    public void verifyConnectionIsMade() throws Exception {
        Main main = new Main(serviceProvider, uiFactory, daoFactory, timerFactory);

        main.run();

        handleAllEdtEvents();

        verify(connection).connect();

    }

    @Test
    public void verifySuccessfulConnectionTriggersMainWindowToBeShown() throws Exception {
        Main main = new Main(serviceProvider, uiFactory, daoFactory, timerFactory);

        main.run();

        handleAllEdtEvents();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.CONNECTED);

        handleAllEdtEvents();

        verify(mainWindowController).showMainMainWindow();
    }

    @Test
    public void verifySuccessfulConnectionRegistersDAOs() throws Exception {

        Main main = new Main(serviceProvider, uiFactory, daoFactory, timerFactory);

        main.run();

        handleAllEdtEvents();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.CONNECTED);

        handleAllEdtEvents();

        verify(daoFactory).registerDAOsAndStorageAsOSGiServices();
    }

    @Ignore("this prompts the user with some gui")
    @Test
    public void verifyFailedConnectionTriggersShutdown() throws Exception {

        Main main = new Main(serviceProvider, uiFactory, daoFactory, timerFactory);

        main.run();

        handleAllEdtEvents();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.FAILED_TO_CONNECT);

        handleAllEdtEvents();

        verify(uiFactory).shutdown();
    }
}
