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

package com.redhat.thermostat.vm.jmx.client.core.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsViewProvider;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;

public class JmxNotificationsViewServiceImplTest {

    private AgentInfoDAO agentDao;
    private VmInfoDAO vmInfoDao;
    private JmxNotificationDAO notificationDao;
    private Timer timer;
    private TimerFactory timerFactory;
    private JmxNotificationsViewProvider viewProvider;
    private JmxNotificationsViewServiceImpl service;
    private JmxNotificationsView view;
    private RequestQueue queue;

    @Before
    public void setUp() {
        ApplicationService appSvc = mock(ApplicationService.class);
        agentDao = mock(AgentInfoDAO.class);
        vmInfoDao = mock(VmInfoDAO.class);
        notificationDao = mock(JmxNotificationDAO.class);
        timer = mock(Timer.class);
        timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);

        queue = mock(RequestQueue.class);

        view = mock(JmxNotificationsView.class);
        viewProvider = mock(JmxNotificationsViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        service = new JmxNotificationsViewServiceImpl(appSvc, agentDao, vmInfoDao, notificationDao,
                queue, timerFactory, viewProvider);
    }

    @Test
    public void verifyGetFilter() {
        assertNotNull(service.getFilter());
    }

    @Test
    public void verifyGetInformationServiceController() {
        VmRef vm = mock(VmRef.class);
        HostRef hostRef = mock(HostRef.class);
        VmInfo vmInfo = mock(VmInfo.class);
        when(hostRef.getAgentId()).thenReturn("agentId");
        when(vm.getHostRef()).thenReturn(hostRef);
        when(vmInfoDao.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);
        when(vmInfo.isAlive(any(AgentInformation.class))).thenReturn(VmInfo.AliveStatus.RUNNING);
        assertNotNull(service.getInformationServiceController(vm));
    }

    @Test
    public void verifyGetOrderValue() {
        assertTrue(service.getOrderValue() > Ordered.ORDER_USER_GROUP);
    }

}

