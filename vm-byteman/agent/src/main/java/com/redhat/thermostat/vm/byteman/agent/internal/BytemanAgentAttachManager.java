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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.byteman.agent.submit.Submit;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.agent.internal.ProcessUserInfoBuilder.ProcessUserInfo;
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
    static final String THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY = "org.jboss.byteman.thermostat.socketName";
    static List<String> helperJars;
    
    private final SubmitHelper submit;
    private final FileSystemUtils fsUtils;
    private BytemanAttacher attacher;
    private IPCEndpointsManager ipcManager;
    private VmBytemanDAO vmBytemanDao;
    private WriterID writerId;
    private ProcessUserInfoBuilder userInfoBuilder;

    BytemanAgentAttachManager() {
        this.submit = new SubmitHelper();
        this.fsUtils = new FileSystemUtils();
    }
    
    // for testing only
    BytemanAgentAttachManager(BytemanAttacher attacher, IPCEndpointsManager ipcManager, VmBytemanDAO vmBytemanDao, SubmitHelper submit, 
            WriterID writerId, ProcessUserInfoBuilder userInfoBuilder, FileSystemUtils fsUtils) {
        this.attacher = attacher;
        this.ipcManager = ipcManager;
        this.vmBytemanDao = vmBytemanDao;
        this.submit = submit;
        this.writerId = writerId;
        this.userInfoBuilder = userInfoBuilder;
        this.fsUtils = fsUtils;
    }
    
    VmBytemanStatus attachBytemanToVm(VmId vmId, int vmPid) {
        logger.fine("Attaching byteman agent to VM '" + vmPid + "'");
        // Fail early if we can't determine process owner
        UserPrincipal owner = getUserPrincipalForPid(vmPid);
        if (owner == null) {
            return null;
        }
        BytemanAgentInfo info = attacher.attach(vmId.get(), vmPid, writerId.getWriterID());
        if (info == null) {
            logger.warning("Failed to attach byteman agent for VM '" + vmPid + "'. Skipping rule updater and IPC channel.");
            return null;
        }
        VmSocketIdentifier socketId = new VmSocketIdentifier(vmId.get(), vmPid, writerId.getWriterID());
        boolean postAttachGood = performPostAttachSteps(info, socketId);
        if (!postAttachGood) {
            return null;
        }
        ThermostatIPCCallbacks callback = new BytemanMetricsReceiver(vmBytemanDao, socketId);
        ipcManager.startIPCEndpoint(socketId, callback, owner);
        // Add a status record to storage
        VmBytemanStatus status = new VmBytemanStatus(writerId.getWriterID());
        status.setListenPort(info.getAgentListenPort());
        status.setTimeStamp(System.currentTimeMillis());
        status.setVmId(vmId.get());
        vmBytemanDao.addOrReplaceBytemanStatus(status);
        return status;
    }
    
    private UserPrincipal getUserPrincipalForPid(int vmPid) {
        UserPrincipal principal = null;
        ProcessUserInfo info = userInfoBuilder.build(vmPid);
        String username = info.getUsername();
        if (username == null) {
            logger.warning("Unable to determine owner of VM '" + vmPid + "'. Skipping rule updater and IPC channel.");
        } else {
            UserPrincipalLookupService lookup = fsUtils.getUserPrincipalLookupService();
            try {
                principal = lookup.lookupPrincipalByName(username);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Invalid user name '" + username + "' for VM '" + vmPid + "'. Skipping rule updater and IPC channel.", e);
            }
        }
        return principal;
    }

    private boolean performPostAttachSteps(BytemanAgentInfo info, VmSocketIdentifier socketId) {
        if (info.isAttachFailedNoSuchProcess()) {
            logger.finest("Process with pid " + info.getVmPid() + " went away before we could attach the byteman agent to it.");
            return false;
        }
        if (info.isOldAttach()) {
            logger.finest("Proceeding with post-attach steps for already attached byteman agent");
            Properties properties = new Properties();
            properties.setProperty(THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY, socketId.getName());
            return submit.setSystemProperties(properties, info);
        } else {
            logger.fine("Attached byteman agent to VM '" + info.getVmPid() + "' at port: '" + info.getAgentListenPort());
            boolean addJarsSuccess = addThermostatHelperJarsToClasspath(info);
            if (!addJarsSuccess) {
                logger.warning("VM '" + info.getVmPid() + "': Failed to add helper jars to target VM's classpath.");
            }
            return addJarsSuccess;
        }
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

    void setUserInfoBuilder(ProcessUserInfoBuilder userInfoBuilder) {
        this.userInfoBuilder = userInfoBuilder;
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
        
        boolean setSystemProperties(Properties properties, BytemanAgentInfo info) {
            Submit submit = new Submit(null /* localhost */, info.getAgentListenPort());
            try {
                String result = submit.setSystemProperties(properties);
                logger.fine("Re-set system properties with result: " + result);
                return true;
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
                return false;
            }
        }
        
    }
    
    static class FileSystemUtils {
        
        UserPrincipalLookupService getUserPrincipalLookupService() {
            return FileSystems.getDefault().getUserPrincipalLookupService();
        }
        
    }
}
