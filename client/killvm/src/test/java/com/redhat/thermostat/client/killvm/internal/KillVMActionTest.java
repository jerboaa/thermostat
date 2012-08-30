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

package com.redhat.thermostat.client.killvm.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.osgi.service.VmFilter;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.OSGIUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(OSGIUtils.class)
public class KillVMActionTest {

    private KillVMAction action;
    private DAOFactory factory;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        factory = mock(DAOFactory.class);
        ApplicationContext.getInstance().setDAOFactory(factory);
        action = new KillVMAction();
    }

    @After
    public void teardown() {
        factory = null;
        action = null;
    }

    @Test
    public void killVMFilterOnlyMatchesLiveVMs() {
        VmFilter filter = action.getFilter();
        VmRef matching = mock(VmRef.class);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        VmInfo vmInfo = mock(VmInfo.class);
        when(factory.getVmInfoDAO()).thenReturn(vmInfoDao);
        when(vmInfoDao.getVmInfo(matching)).thenReturn(vmInfo);
        when(vmInfo.isAlive()).thenReturn(true);
        assertTrue(filter.matches(matching));
        when(vmInfo.isAlive()).thenReturn(false);
        assertFalse(filter.matches(matching));
    }

    @Test
    public void canQueueKillRequest() {
        Storage storage = mock(Storage.class);
        when(factory.getStorage()).thenReturn(storage);
        VmRef ref = mock(VmRef.class);
        HostRef hostref = mock(HostRef.class);
        when(ref.getAgent()).thenReturn(hostref);
        String agentAddress = "127.0.0.1:8888";
        when(storage.getConfigListenAddress(hostref)).thenReturn(agentAddress);
        final Request req = mock(Request.class);
        KillVMAction action = new KillVMAction() {
            @Override
            Request getKillRequest(InetSocketAddress target) {
                return req;
            }
        };
        OSGIUtils utils = mock(OSGIUtils.class);
        PowerMockito.mockStatic(OSGIUtils.class);
        when(OSGIUtils.getInstance()).thenReturn(utils);
        RequestQueue queue = mock(RequestQueue.class);
        when(utils.getService(RequestQueue.class)).thenReturn(queue);
        action.execute(ref);
        ArgumentCaptor<String> vmIdParamCaptor = ArgumentCaptor
                .forClass(String.class);
        verify(req).setParameter(vmIdParamCaptor.capture(), any(String.class));
        assertEquals("vm-id", vmIdParamCaptor.getValue());
        verify(req).addListener(isA(VMKilledListener.class));
        ArgumentCaptor<String> receiverCaptor = ArgumentCaptor
                .forClass(String.class);
        verify(req).setReceiver(receiverCaptor.capture());
        assertEquals(
                "com.redhat.thermostat.agent.killvm.internal.KillVmReceiver",
                receiverCaptor.getValue());
        verify(queue).putRequest(req);
    }

}
