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

package com.redhat.thermostat.client.filter.internal.vm.swing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class VMPidLabelDecoratorTest {

    private VmInfoDAO dao;
    private VMPidLabelDecorator decorator;

    @Before
    public void setup() {
        dao = mock(VmInfoDAO.class);
        decorator = new VMPidLabelDecorator(dao);
    }

    @Test
    public void testGetLabel() {
        decorator.setEnabled(true);

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.getVmPid()).thenReturn(100);
        when(dao.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);

        VmRef ref = mock(VmRef.class);
        String str = decorator.getLabel("Foo", ref);
        assertThat(str, containsString("Foo"));
        assertThat(str, containsString("PID"));
        assertThat(str, containsString("100"));
    }

    @Test
    public void verifyGetLabelCaches() {
        decorator.setEnabled(true);
        when(dao.getVmInfo(isA(VmRef.class))).thenReturn(mock(VmInfo.class));
        VmRef ref = mock(VmRef.class);

        decorator.getLabel("", ref);
        verify(dao).getVmInfo(ref);

        decorator.getLabel("", ref);
        verify(dao).getVmInfo(ref); // still only once -> cached after first call
    }

    @Test
    public void verifyNoDaoAccessWhenDisabled() {
        decorator.setEnabled(false);
        decorator.getLabel("", mock(VmRef.class));
        verifyZeroInteractions(dao);
    }

    @Test
    public void verifyNoDaoAccessWhenWrongRefType() {
        decorator.setEnabled(true);
        decorator.getLabel("", mock(HostRef.class));
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
