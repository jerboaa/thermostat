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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.osgi.service.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.model.NetworkInterfaceInfo;

public class HostOverviewControllerTest {

    private static final String HOST_NAME = "host-name";
    private static final String OS_NAME = "some os";
    private static final String KERNEL_NAME = "korn";
    private static final int CPU_COUNT = 99;
    private static final String CPU_MODEL = "cpu-model";
    private static final long TOTAL_MEMORY = 99+99;
    private static final String NETWORK_INTERFACE = "iface0";
    private static final String IPV4_ADDR = "0xcafef00d";
    private static final String IPV6_ADDR = "HOME_SWEET_HOME";

    private Timer timer;
    private Runnable timerAction;
    private HostOverviewView view;
    private ActionListener<HostOverviewView.Action> listener;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // Setup timer
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        // Setup DAOs
        HostInfo hostInfo = new HostInfo(HOST_NAME, OS_NAME, KERNEL_NAME, CPU_MODEL, CPU_COUNT, TOTAL_MEMORY);

        List<NetworkInterfaceInfo> networkInfo = new ArrayList<NetworkInterfaceInfo>();
        NetworkInterfaceInfo ifaceInfo = new NetworkInterfaceInfo(NETWORK_INTERFACE);
        ifaceInfo.setIp4Addr(IPV4_ADDR);
        ifaceInfo.setIp6Addr(IPV6_ADDR);
        networkInfo.add(ifaceInfo);

        HostRef ref = mock(HostRef.class);

        DAOFactory daoFactory = mock(DAOFactory.class);

        HostInfoDAO hostInfoDao = mock(HostInfoDAO.class);
        when(hostInfoDao.getHostInfo(any(HostRef.class))).thenReturn(hostInfo);

        NetworkInterfaceInfoDAO networkInfoDao = mock(NetworkInterfaceInfoDAO.class);
        when(networkInfoDao.getNetworkInterfaces(any(HostRef.class))).thenReturn(networkInfo);

        when(daoFactory.getHostInfoDAO()).thenReturn(hostInfoDao);
        when(daoFactory.getNetworkInterfaceInfoDAO()).thenReturn(networkInfoDao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        // Setup View
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        view = mock(HostOverviewView.class);
        doNothing().when(view).addActionListener(listenerCaptor.capture());
        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(HostOverviewView.class))).thenReturn(view);

        ApplicationContext.getInstance().setViewFactory(viewFactory);

        HostOverviewController controller = new HostOverviewController(ref);

        listener = listenerCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyViewIsUpdatedWithData() {
        timerAction.run();

        verify(view).setCpuCount(eq(String.valueOf(CPU_COUNT)));
        verify(view).setCpuModel(eq(CPU_MODEL));
        verify(view).setHostName(eq(HOST_NAME));

        verify(view).setNetworkTableColumns(any(String[][].class));
        verify(view).setInitialNetworkTableData(eq(new String[][] { new String[] { NETWORK_INTERFACE, IPV4_ADDR, IPV6_ADDR }, }));

        verify(view).setOsKernel(eq(KERNEL_NAME));
        verify(view).setOsName(eq(OS_NAME));
        verify(view).setTotalMemory(eq(String.valueOf(TOTAL_MEMORY)));
    }

    @Test
    public void verifyTimerIsSetUpCorrectly() {
        assertNotNull(timer);

        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
    }

    @Test
    public void verifyTimerRunsWhenNeeded() {
        listener.actionPerformed(new ActionEvent<>(view, Action.VISIBLE));

        verify(timer).start();

        listener.actionPerformed(new ActionEvent<>(view, Action.HIDDEN));

        verify(timer).stop();
    }

}
