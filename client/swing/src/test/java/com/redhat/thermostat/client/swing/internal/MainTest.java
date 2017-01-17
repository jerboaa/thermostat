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

package com.redhat.thermostat.client.swing.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.swing.JOptionPane;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.swing.internal.Main.ClientPreferencesCreator;
import com.redhat.thermostat.client.swing.internal.Main.GUIInteractions;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;

public class MainTest {

    private ArgumentCaptor<ConnectionListener> connectionListenerCaptor;

    private DbServiceFactory dbServiceFactory;
    private DbService dbService;

    private ExecutorService executorService;
    private TimerFactory timerFactory;
    private StubBundleContext context;
    private ApplicationService appService;
    private Keyring keyring;
    private CountDownLatch shutdown;

    private ClientPreferencesCreator prefsCreator;
    private ClientPreferences prefs;

    private GUIInteractions interactions;

    @Before
    public void setUp() {
        context = new StubBundleContext();
        appService = mock(ApplicationService.class);
        context.registerService(ApplicationService.class, appService, null);

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
        
        dbServiceFactory = mock(DbServiceFactory.class);
        dbService = mock(DbService.class);
        when(dbServiceFactory.createDbService(anyString(), any(StorageCredentials.class), any(SSLConfiguration.class))).thenReturn(dbService);
        
        connectionListenerCaptor = ArgumentCaptor.forClass(ConnectionListener.class);
        doNothing().when(dbService).addConnectionListener(connectionListenerCaptor.capture());

        timerFactory = mock(TimerFactory.class);
        when(appService.getTimerFactory()).thenReturn(timerFactory);

        keyring = mock(Keyring.class);

        prefs = mock(ClientPreferences.class);

        prefsCreator = mock(ClientPreferencesCreator.class);
        when(prefsCreator.create()).thenReturn(prefs);

        shutdown = mock(CountDownLatch.class);

        interactions = mock(GUIInteractions.class);
    }

    private Main createMain(BundleContext context) {
        return new Main(context, appService, dbServiceFactory, keyring, shutdown, prefsCreator, interactions);
    }

    @Test
    public void verifyLookAndFeelIsSet() throws Exception {
        Main main = createMain(context);

        main.run();

        verify(interactions).initializeLookAndFeel();
    }

    @Test
    public void verifyRunWaitsForShutdown() throws Exception {
        Main main = createMain(context);

        main.run();

        verify(shutdown).await();
    }

    @Test
    public void verifyUnknownStorageProtocolIsHandledCorrectly() {
        StorageException unknownProtocolException = new StorageException("Unknown protocol");
        when(dbServiceFactory.createDbService(anyString(), any(StorageCredentials.class), any(SSLConfiguration.class))).thenThrow(unknownProtocolException);

        Main main = createMain(context);

        main.run();

        verify(interactions).showFailedToConnectDialogWithRetryOption();
    }

    @Test
    public void verifyConnectionIsMade() throws Exception {
        Main main = createMain(context);

        main.run();

        verify(dbService).connect();
    }

    @Test
    public void verifySuccessfulConnectionTriggersMainWindowToBeShown() throws Exception {
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        context.registerService(HostInfoDAO.class, hostInfoDAO, null);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        context.registerService(VmInfoDAO.class, vmInfoDAO, null);

        Main main = createMain(context);

        main.run();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.CONNECTED);

        verify(interactions).showMainWindow();
    }

    @Test
    public void verifyFailedConnectionTriggersMessageWithQuitOption() throws Exception {
        Main main = createMain(context);

        when(interactions.showFailedToConnectDialogWithRetryOption()).thenReturn(JOptionPane.CANCEL_OPTION);

        main.run();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.FAILED_TO_CONNECT);

        verify(interactions).showFailedToConnectDialogWithRetryOption();
        verify(shutdown).countDown();

        verify(interactions, never()).showPreferencesDialog(any(ExecutorService.class));
    }

    @Test
    public void verifyFailedConnectionTriggersMessageWithRetryOption() throws Exception {
        Main main = createMain(context);

        when(interactions.showFailedToConnectDialogWithRetryOption()).thenReturn(JOptionPane.YES_OPTION);

        main.run();

        ConnectionListener connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.FAILED_TO_CONNECT);

        verify(interactions).showFailedToConnectDialogWithRetryOption();
        verify(interactions, never()).showFailedToConnectDialog();
        verify(interactions).showPreferencesDialog(any(ExecutorService.class));

        main.reconnect(prefs, mock(StorageCredentials.class));

        connectionListener = connectionListenerCaptor.getValue();
        connectionListener.changed(ConnectionStatus.FAILED_TO_CONNECT);

        verify(interactions).showFailedToConnectDialog();
    }
}

