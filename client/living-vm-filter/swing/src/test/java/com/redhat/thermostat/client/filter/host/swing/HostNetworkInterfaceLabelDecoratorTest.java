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

package com.redhat.thermostat.client.filter.host.swing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.VmRef;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class HostNetworkInterfaceLabelDecoratorTest {

    private NetworkInterfaceInfoDAO dao;
    private HostNetworkInterfaceLabelDecorator decorator;

    @Before
    public void setup() {
        dao = mock(NetworkInterfaceInfoDAO.class);
        decorator = new HostNetworkInterfaceLabelDecorator(dao);
    }

    @Test
    public void testBasicWithNetworkInterfaces() {
        List<NetworkInterfaceInfo> networkList = new ArrayList<>();
        String ip = "192.168.0.1";
        NetworkInterfaceInfo info = new NetworkInterfaceInfo("foo-agent", ip);
        assertEquals(ip, info.getInterfaceName());
        info.setIp4Addr(ip);
        networkList.add(info);
        HostRef mockHostRef = mock(HostRef.class);
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(any(HostRef.class))).thenReturn(networkList);
        String decoratedLabel = decorator.getLabel("", mockHostRef).trim();
        assertEquals("192.168.0.1", decoratedLabel);
    }
    
    @Test
    public void testBasicTwoIfaces() {
        List<NetworkInterfaceInfo> networkList = new ArrayList<>();
        String ip = "192.168.0.1";
        String ip2 = "10.0.0.1";
        NetworkInterfaceInfo info = new NetworkInterfaceInfo("foo-agent", ip);
        NetworkInterfaceInfo info2 = new NetworkInterfaceInfo("foo-agent", ip2);
        assertEquals(ip2, info2.getInterfaceName());
        assertEquals(ip, info.getInterfaceName());
        info.setIp4Addr(ip);
        info2.setIp4Addr(ip2);
        networkList.add(info);
        networkList.add(info2);
        HostRef mockHostRef = mock(HostRef.class);
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(any(HostRef.class))).thenReturn(networkList);
        String decoratedLabel = decorator.getLabel("", mockHostRef).trim();
        assertEquals("192.168.0.1; 10.0.0.1", decoratedLabel);
    }
    
    @Test
    public void testWithEmptyIp4AddrAndNoIPv6Addr() {
        List<NetworkInterfaceInfo> networkList = new ArrayList<>();
        String ip = "192.168.0.1";
        NetworkInterfaceInfo info = new NetworkInterfaceInfo("foo-agent", ip);
        assertEquals(ip, info.getInterfaceName());
        info.setIp4Addr(""); // empty string
        networkList.add(info);
        HostRef mockHostRef = mock(HostRef.class);
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(any(HostRef.class))).thenReturn(networkList);
        String decoratedLabel = decorator.getLabel("", mockHostRef).trim();
        assertEquals("", decoratedLabel);
    }
    
    /**
     * This should not throw IndexOutOfBoundsException
     */
    @Test
    public void testWithNoNetworkInterfaces() {
        List<NetworkInterfaceInfo> networkList = new ArrayList<>();
        HostRef mockHostRef = mock(HostRef.class);
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(any(HostRef.class))).thenReturn(networkList);
        String decoratedLabel = decorator.getLabel("", mockHostRef).trim();
        assertEquals("", decoratedLabel);
    }

    @Test
    public void verifyGetLabelCaches() {
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(isA(HostRef.class))).thenReturn(new ArrayList<NetworkInterfaceInfo>());
        HostRef ref = mock(HostRef.class);

        decorator.getLabel("", ref);
        verify(dao).getNetworkInterfaces(ref);

        decorator.getLabel("", ref);
        verify(dao).getNetworkInterfaces(ref); // still only once -> cached after first call
    }

    @Test
    public void verifyGetLabelAppends() {
        decorator.setEnabled(true);
        when(dao.getNetworkInterfaces(isA(HostRef.class))).thenReturn(new ArrayList<NetworkInterfaceInfo>());
        HostRef ref = mock(HostRef.class);

        String str = decorator.getLabel("Foo", ref).trim();
        assertThat(str, containsString("Foo"));
    }

    @Test
    public void verifyNoDaoAccessWhenDisabled() {
        decorator.setEnabled(false);
        decorator.getLabel("", mock(HostRef.class));
        verifyZeroInteractions(dao);
    }

    @Test
    public void verifyNoDaoAccessWhenWrongRefType() {
        decorator.setEnabled(true);
        decorator.getLabel("", mock(VmRef.class));
        verifyZeroInteractions(dao);
    }

    @Test
    public void testSetEnabled() {
        decorator.setEnabled(true);
        assertThat(decorator.isEnabled(), is(true));
        decorator.setEnabled(false);
        assertThat(decorator.isEnabled(), is(false));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testTogglingEnabledFiresEvent() {
        ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> listener =
                (ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent>) mock(ActionListener.class);

        decorator.addStatusEventListener(listener);

        decorator.setEnabled(!decorator.isEnabled());

        ArgumentCaptor<ActionEvent> captor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(listener).actionPerformed(captor.capture());

        ActionEvent event = captor.getValue();
        assertThat((ToggleableReferenceFieldLabelDecorator.StatusEvent) event.getActionId(),
                is(ToggleableReferenceFieldLabelDecorator.StatusEvent.STATUS_CHANGED));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSettingSameEnabledValueDoesNotFireEvent() {
        ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent> listener =
                (ActionListener<ToggleableReferenceFieldLabelDecorator.StatusEvent>) mock(ActionListener.class);

        decorator.addStatusEventListener(listener);

        decorator.setEnabled(decorator.isEnabled());

        verify(listener, never()).actionPerformed(Matchers.<ActionEvent<ToggleableReferenceFieldLabelDecorator.StatusEvent>>any());
    }

}
