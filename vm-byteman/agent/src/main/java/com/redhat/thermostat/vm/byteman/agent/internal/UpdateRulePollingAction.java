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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;

import com.redhat.thermostat.backend.VmPollingAction;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.shared.config.CommonPaths;

class UpdateRulePollingAction implements VmPollingAction {
    
    private static final String BYTEMAN_PLUGIN_DIR = System.getProperty("thermostat.plugin", "vm-byteman");
    private static final String BYTEMAN_PLUGIN_LIBS_DIR = BYTEMAN_PLUGIN_DIR + File.separator + "plugin-libs";
    private static final String BYTEMAN_HELPER_DIR = BYTEMAN_PLUGIN_LIBS_DIR + File.separator + "thermostat-helper";
    private static final String BYTEMAN_SCRIPT_NAME = "bytemanRule.btm";
    private static final Logger logger = LoggingUtils.getLogger(UpdateRulePollingAction.class);
    static final List<ScriptText> bmScripts;
    static {
        bmScripts = getBmScripts();
    }
    
    private static List<String> helperJars = null;
    private final BytemanAgentInfo agentInfo;
    private final Map<String, Boolean> rulesSubmitted;
    private boolean loggedRuleSubmission = false;
    
    UpdateRulePollingAction(BytemanAgentInfo agentInfo, CommonPaths paths) {
        this.agentInfo = agentInfo;
        this.rulesSubmitted = new HashMap<>();
        File bytemanHelperDir = new File(paths.getSystemPluginRoot(), BYTEMAN_HELPER_DIR);
        initListOfHelperJars(bytemanHelperDir);
    }


    @Override
    public void run(String vmId, int pid) {
        // As of yet we submit the fixed rule once and be done with it.
        // A future enhancement, for example, would be to put the rule
        // into storage, retrieve it and if changed submit the changed
        // rules.
        Boolean value = rulesSubmitted.get(vmId);
        boolean ruleSubmitted = value == null ? false : value;
        if (!ruleSubmitted) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            PrintStream bytemanOutputWriter = new PrintStream(bout); 
            String host = agentInfo.getListenHost();
            int port = agentInfo.getAgentListenPort();
            Submit bytemanSubmit = new Submit(host, port, bytemanOutputWriter);
            try {
                // Add jar files for Thermostat byteman helper:
                String addJarsResult = bytemanSubmit.addJarsToSystemClassloader(helperJars);
                logger.fine("Added jars for byteman helper with result: " + addJarsResult);
                List<ScriptText> existingScripts = bytemanSubmit.getAllScripts();
                if (existingScripts.size() > 0) {
                    String deleteResult = bytemanSubmit.deleteScripts(bmScripts);
                    logger.fine("Deleted rule with result: " + deleteResult);
                }
                String deployResult = bytemanSubmit.addScripts(bmScripts);
                logger.fine("Deployed rule with result: " + deployResult);
                logger.finest("Byteman output for VM ID '" + vmId + "' was: " + new String(bout.toByteArray()));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to submit rules.", e);
            }
            rulesSubmitted.put(vmId, true);
        } else {
            if (!loggedRuleSubmission) {
                logger.fine("Skipping byteman rule submission for VM '" + pid + "', since it's been submitted already.");
                loggedRuleSubmission = true;
            }
        }
    }
    
    private static List<ScriptText> getBmScripts() {
        String scriptText = getScriptText();
        ScriptText text = new ScriptText(BYTEMAN_SCRIPT_NAME, scriptText);
        List<ScriptText> bmlist = new ArrayList<>();
        bmlist.add(text);
        return bmlist;
    }

    private static String getScriptText() {
        try {
            byte[] bytes = StreamUtils.readAll(UpdateRulePollingAction.class.getResourceAsStream("/" + BYTEMAN_SCRIPT_NAME));
            String script = new String(bytes, Charset.forName("UTF-8"));
            return script.trim(); // Byteman does not seem to like empty lines at the end of rule files
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read byteman rule file", e);
            return null;
        }
    }
    
    // package private for testing
    static List<String> initListOfHelperJars(File helperDir) {
        if (helperJars == null) {
            List<String> jars = new ArrayList<>();
            for (File f: helperDir.listFiles()) {
                jars.add(f.getAbsolutePath());
            }
            helperJars = jars;
        }
        return helperJars;
    }

}
