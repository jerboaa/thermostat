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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.utils.ProcessChecker;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.agent.internal.BytemanAgentAttachManager.SubmitHelper;
import com.redhat.thermostat.vm.byteman.agent.internal.BytemanAttacher.BtmInstallHelper;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

public class VmBytemanBackendTest {
    
    private CommonPaths paths;
    private String filePath;
    private ProcessChecker processChecker;
    
    @Before
    public void setup() {
        filePath = "/path/to/run/data";
        paths = mock(CommonPaths.class);
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(paths.getUserIPCConfigurationFile()).thenReturn(mockFile);

        processChecker = mock(ProcessChecker.class);
        when(processChecker.exists(anyInt())).thenReturn(false);
    }

    /**
     * Ensure that no NPE is being thrown when attaching the byteman
     * agent fails.
     */
    @Test
    public void verifyAttachFailureNotCausingNPE() throws Exception {
        BtmInstallHelper installer = mock(BtmInstallHelper.class);
        IOException ioe = new IOException("Something not accounted for");
        when(installer.install(any(String.class), any(Boolean.class), any(Boolean.class), any(String.class), any(Integer.class), any(String[].class))).thenThrow(ioe);
        BytemanAttacher attacher = new BytemanAttacher(installer, processChecker, paths);
        BytemanAgentAttachManager attachManager = new BytemanAgentAttachManager(attacher, mock(IPCEndpointsManager.class), mock(VmBytemanDAO.class), mock(SubmitHelper.class), mock(WriterID.class));
        VmBytemanBackend backend = new VmBytemanBackend(attachManager, true);
        WriterID writerId = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn("some-writer-id");
        backend.bindWriterId(writerId);
        backend.attachBytemanToVm("someVmId", -1); // must not throw NPE
    }
}
