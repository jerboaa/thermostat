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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;

/**
 * Receiver class for Byteman action command channel requests.
 *
 */
@Component
@Service(value = RequestReceiver.class)
@Property(name = "servicename", value = "com.redhat.thermostat.vm.byteman.agent.internal.BytemanRequestReceiver")
public class BytemanRequestReceiver implements RequestReceiver {
    
    private static final Logger logger = LoggingUtils.getLogger(BytemanRequestReceiver.class);
    private static final Response ERROR_RESPONSE = new Response(ResponseType.ERROR);
    private static final Response OK_RESPONSE = new Response(ResponseType.OK);
    
    
    @Reference
    private VmBytemanDAO vmBytemanDao;
    
    @Reference
    private WriterID writerId;

    ////////////////////////////////////////////////
    // methods used by DS
    ////////////////////////////////////////////////
    
    protected void bindWriterId(WriterID writerId) {
        this.writerId = writerId;
    }
    
    protected void unbindWriterId(WriterID writerId) {
        this.writerId = null;
    }
    
    protected void bindVmBytemanDao(VmBytemanDAO dao) {
        this.vmBytemanDao = dao;
    }
    
    protected void unbindVmBytemanDao(VmBytemanDAO dao) {
        this.vmBytemanDao = null;
    }
    
    ////////////////////////////////////////////////
    // end methods used by DS
    ////////////////////////////////////////////////
    
    @Override
    public Response receive(Request request) {
        String vmId = request.getParameter(BytemanRequest.VM_ID_PARAM_NAME);
        String actionStr = request.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        String portStr = request.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME);
        RequestAction action;
        try {
            action = RequestAction.fromIntString(actionStr);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Illegal action received", e);
            return ERROR_RESPONSE;
        }
        logger.fine("Processing request for vmId: " + vmId + ", Action: " + action + ", port: " + portStr);
        int port = Integer.parseInt(portStr);
        Response response = null;
        switch(action) {
        case LOAD_RULES:
            String rule = request.getParameter(BytemanRequest.RULE_PARAM_NAME);
            response = loadRules(port, new VmId(vmId), rule);
            break;
        case UNLOAD_RULES:
            response = unloadRules(port, new VmId(vmId));
            break;
        default:
            logger.warning("Unknown action: " + action);
            return ERROR_RESPONSE;
        }
        return response;
    }

    private Response loadRules(int listenPort, VmId vmId, String bytemanRules) {
        Submit submit = getSubmit(listenPort);
        try {
            List<ScriptText> existingScripts = submit.getAllScripts();
            if (existingScripts.size() > 0) {
                String deleteResult = submit.deleteAllRules();
                logger.fine("Deleted rules with result: " + deleteResult);
            }
            List<InputStream> list = Arrays.<InputStream>asList(new ByteArrayInputStream(bytemanRules.getBytes(Charset.forName("UTF-8"))));
            String addRulesResult = submit.addRulesFromResources(list);
            logger.info("Added byteman rules for VM with id '" + vmId.get() + "' with result: " + addRulesResult);
            VmBytemanStatus status = new VmBytemanStatus(writerId.getWriterID());
            status.setListenPort(listenPort);
            status.setRule(bytemanRules);
            status.setTimeStamp(System.currentTimeMillis());
            status.setVmId(vmId.get());
            vmBytemanDao.addOrReplaceBytemanStatus(status);
            return OK_RESPONSE;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to submit byteman rules", e);
            return ERROR_RESPONSE;
        }
    }

    private Response unloadRules(int listenPort, VmId vmId) {
        Submit submit = getSubmit(listenPort);
        try {
            List<ScriptText> list = submit.getAllScripts();
            // Avoid no scripts to delete errors
            if (!list.isEmpty()) {
                submit.deleteAllRules();
                // update the corresponding status in storage
                VmBytemanStatus newStatus = new VmBytemanStatus(writerId.getWriterID());
                newStatus.setListenPort(listenPort);
                newStatus.setRule(null);
                newStatus.setTimeStamp(System.currentTimeMillis());
                newStatus.setVmId(vmId.get());
                vmBytemanDao.addOrReplaceBytemanStatus(newStatus);
            }
            return OK_RESPONSE;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete rules", e);
            return ERROR_RESPONSE;
        }
    }

    protected Submit getSubmit(int listenPort) {
        return new Submit(null /* localhost */, listenPort);
    }
    
}
