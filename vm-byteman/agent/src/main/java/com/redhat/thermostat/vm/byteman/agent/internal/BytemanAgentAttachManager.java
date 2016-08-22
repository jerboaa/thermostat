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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.byteman.agent.submit.Submit;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;

/**
 * Manages attaching of the byteman agent as well as adding
 * helper jars to the target JVMs classpath.
 *
 */
class BytemanAgentAttachManager {
    
    private static final String BYTEMAN_PLUGIN_DIR = System.getProperty("thermostat.plugin", "vm-byteman");
    private static final String BYTEMAN_PLUGIN_LIBS_DIR = BYTEMAN_PLUGIN_DIR + File.separator + "plugin-libs";
    private static final String BYTEMAN_INSTALL_HOME = BYTEMAN_PLUGIN_LIBS_DIR + File.separator + "byteman-install";
    private static final String BYTEMAN_HELPER_DIR = BYTEMAN_PLUGIN_LIBS_DIR + File.separator + "thermostat-helper";
    private static final String BYTEMAN_HOME_PROPERTY = "org.jboss.byteman.home";
    private static final Logger logger = LoggingUtils.getLogger(BytemanAgentAttachManager.class);
    // package-private for testing
    static List<String> helperJars;
    
    private final SubmitHelper submit;
    private BytemanAttacher attacher;
    private IPCEndpointsManager ipcManager;
    private VmBytemanDAO vmBytemanDao;
    private WriterID writerId;

    BytemanAgentAttachManager() {
        this.submit = new SubmitHelper();
    }
    
    // for testing only
    BytemanAgentAttachManager(BytemanAttacher attacher, IPCEndpointsManager ipcManager, VmBytemanDAO vmBytemanDao, SubmitHelper submit, WriterID writerId) {
        this.attacher = attacher;
        this.ipcManager = ipcManager;
        this.vmBytemanDao = vmBytemanDao;
        this.submit = submit;
        this.writerId = writerId;
    }
    
    VmBytemanStatus attachBytemanToVm(VmId vmId, int vmPid) {
        logger.fine("Attaching byteman agent to VM '" + vmPid + "'");
        BytemanAgentInfo info = attacher.attach(vmId.get(), vmPid, writerId.getWriterID());
        if (info == null) {
            logger.warning("Failed to attach byteman agent for VM '" + vmPid + "'. Skipping rule updater and IPC channel.");
            return null;
        }
        if (info.isAttachFailedNoSuchProcess()) {
            logger.finest("Process with pid " + vmPid + " went away before we could attach the byteman agent to it.");
            return null;
        }
        logger.fine("Attached byteman agent to VM '" + vmPid + "' at port: '" + info.getAgentListenPort());
        if (!addThermostatHelperJarsToClasspath(info)) {
            logger.warning("VM '" + vmPid + "': Failed to add helper jars to target VM's classpath.");
            return null;
        }
        VmSocketIdentifier socketId = new VmSocketIdentifier(vmId.get(), vmPid, writerId.getWriterID());
        ThermostatIPCCallbacks callback = new BytemanMetricsReceiver(vmBytemanDao, socketId);
        ipcManager.startIPCEndpoint(socketId, callback);
        // Add a status record to storage
        VmBytemanStatus status = new VmBytemanStatus(writerId.getWriterID());
        status.setListenPort(info.getAgentListenPort());
        status.setTimeStamp(System.currentTimeMillis());
        status.setVmId(vmId.get());
        vmBytemanDao.addOrReplaceBytemanStatus(status);
        return status;
    }
    
    private boolean addThermostatHelperJarsToClasspath(BytemanAgentInfo info) {
        return submit.addJarsToSystemClassLoader(helperJars, info);
    }
    
    static List<String> initListOfHelperJars(CommonPaths commonPaths) {
        File bytemanHelperDir = new File(commonPaths.getSystemPluginRoot(), BYTEMAN_HELPER_DIR);
        return initListOfHelperJars(bytemanHelperDir);
    }
    
    // package private for testing
    static synchronized List<String> initListOfHelperJars(File helperDir) {
        if (helperJars == null) {
            List<String> jars = new ArrayList<>();
            for (File f: helperDir.listFiles()) {
                jars.add(f.getAbsolutePath());
            }
            helperJars = jars;
        }
        return helperJars;
    }
    
    static synchronized void setBytemanHomeProperty(CommonPaths commonPaths) {
        final String bytemanHomePropVal = System.getProperty(BYTEMAN_HOME_PROPERTY);
        if (bytemanHomePropVal == null) {
            String bytemanHome = commonPaths.getSystemPluginRoot().getAbsolutePath() + File.separator + BYTEMAN_INSTALL_HOME;
            // This will depend on BYTEMAN-303 being fixed and incorporated in a release
            logger.fine("Setting system property " + BYTEMAN_HOME_PROPERTY + "=" + bytemanHome);
            System.setProperty(BYTEMAN_HOME_PROPERTY, bytemanHome);
        }
    }
    
    void setAttacher(BytemanAttacher attacher) {
        this.attacher = attacher;
    }

    void setPaths(CommonPaths paths) {
        initListOfHelperJars(paths);
        setBytemanHomeProperty(paths);
    }

    void setIpcManager(IPCEndpointsManager ipcManager) {
        this.ipcManager = ipcManager;
    }

    void setVmBytemanDao(VmBytemanDAO vmBytemanDao) {
        this.vmBytemanDao = vmBytemanDao;
    }

    void setWriterId(WriterID writerId) {
        this.writerId = writerId;
    }

    static class SubmitHelper {
        
        boolean addJarsToSystemClassLoader(List<String> jars, BytemanAgentInfo info) {
            Submit submit = new Submit(null /* localhost */, info.getAgentListenPort());
            try {
                String addJarsResult = submit.addJarsToSystemClassloader(jars);
                logger.fine("Added jars for byteman helper with result: " + addJarsResult);
                return true;
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
                return false;
            }
        }
        
    }
}
