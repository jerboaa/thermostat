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

package com.redhat.thermostat.vm.byteman.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

public class BytemanMetricsReceiverTest {

    private static final int SOME_PID = 23;

    @Test
    public void canSendDataToStorage() {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        ArgumentCaptor<BytemanMetric> metricsCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        VmSocketIdentifier sockId = new VmSocketIdentifier("vm-id", SOME_PID, "agent-id");
        BytemanMetricsReceiver receiver = new BytemanMetricsReceiver(dao, sockId);
        String jsonString = JsonHelper.buildJsonArray(3);
        receiver.dataReceived(jsonString.getBytes());
        verify(dao, times(3)).addMetric(metricsCaptor.capture());
        List<BytemanMetric> metrics = metricsCaptor.getAllValues();
        assertEquals("vm-id", metrics.get(0).getVmId());
        assertEquals("agent-id", metrics.get(2).getAgentId());
        assertTrue(metrics.get(1).getTimeStamp() > 0);
        assertEquals("baz0", metrics.get(0).getMarker());
        assertNotNull(metrics.get(2).getData());
    }
    
}
