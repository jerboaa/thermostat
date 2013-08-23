/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Categories;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BasePojo;

public class CleanDataCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(CleanDataCommand.class);
    private BundleContext bundleContext;
    private boolean removeLiveAgent = false;

    CleanDataCommand (BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference storageServiceRef = bundleContext.getServiceReference(Storage.class);
        if (storageServiceRef == null) {
            throw new CommandException(translator.localize(LocaleResources.STORAGE_UNAVAILABLE));
        }
        Storage storage = (Storage) bundleContext.getService(storageServiceRef);
        
        try {
            Arguments args = ctx.getArguments();
            List<String> agentIdList = args.getNonOptionArguments();
            removeLiveAgent = args.hasArgument(CleanOptions.ALIVE.option);
            PrintStream output = ctx.getConsole().getOutput();
            
            if (args.hasArgument(CleanOptions.ALL.option)) {
                removeDataForAllAgents(storage, output);
            } else {
                removeDataForSpecifiedAgents(storage, agentIdList, output);
            }
        } finally {
            bundleContext.ungetService(storageServiceRef);
        }
    }

    public void removeDataForSpecifiedAgents(Storage storage, List <String> agentIdList, PrintStream output) throws CommandException {
        ServiceReference agentServiceRef = bundleContext.getServiceReference(AgentInfoDAO.class);
        if (agentServiceRef == null) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_UNAVAILABLE));
        }
        AgentInfoDAO agentInfoDAO = (AgentInfoDAO) bundleContext.getService(agentServiceRef);
        
        try {
            Set<String> storedAgentIdList = getAllRegisteredAgents(storage);
            
            for (String agentId : agentIdList) {
                AgentInformation agentInfo = agentInfoDAO.getAgentInformation(new HostRef(agentId, agentId));
                if (agentInfo != null) {
                    removeAgentDataIfSane(storage, agentId, !agentInfo.isAlive(), output);
                } else if (storedAgentIdList.contains(agentId)) {
                    removeAgentDataIfSane(storage, agentId, true, output);
                } else {
                    output.println(translator.localize(LocaleResources.AGENT_NOT_FOUND, agentId).getContents());
                }
            }
        } finally {
            bundleContext.ungetService(agentServiceRef);
        }
    }

    public void removeDataForAllAgents(Storage storage, PrintStream output) throws CommandException {
        ServiceReference agentServiceRef = bundleContext.getServiceReference(AgentInfoDAO.class);
        if (agentServiceRef == null) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_UNAVAILABLE));
        }
        AgentInfoDAO agentInfoDAO = (AgentInfoDAO) bundleContext.getService(agentServiceRef);
        
        try {
            Set<String> storedAgentIdList = getAllRegisteredAgents(storage);
            List<String> aliveAgentsId = new ArrayList<String>();
            List<AgentInformation> allAliveAgentsInfo = agentInfoDAO.getAliveAgents(); 
            for (AgentInformation aliveAgent : allAliveAgentsInfo) {
                aliveAgentsId.add(aliveAgent.getAgentId());
            }
            
            for (String agentId : storedAgentIdList) {
                boolean isDead = !aliveAgentsId.contains(agentId);
                removeAgentDataIfSane(storage, agentId, isDead, output);
            }
        } finally {
            bundleContext.ungetService(agentServiceRef);
        }
    }

    private void removeAgentDataIfSane(Storage storage, String agentId, boolean isDead, PrintStream output) {
        if (isDead || removeLiveAgent) {
            output.println(translator.localize(LocaleResources.PURGING_AGENT_DATA).getContents() + agentId);
            storage.purge(agentId);
        } else {
            output.println(translator.localize(LocaleResources.CANNOT_PURGE_AGENT_RUNNING, agentId).getContents());
        }
    }

    private enum CleanOptions {
        ALL("all"),
        
        ALIVE("alive");
        
        private String option;
        
        CleanOptions(String option) {
            this.option = option;
        }
    }

    private Set<String> getAllRegisteredAgents(Storage storage) {
        List<Category<?>> categories = Categories.getAllCategories();
        Set<String> agents = new HashSet<>();
        PreparedStatement<BasePojo> prepared = null;
        Cursor<BasePojo> agentCursor = null;
        for (Category category : categories) {
            String query = "QUERY " + category.getName();
            StatementDescriptor<BasePojo> desc = new StatementDescriptor<>(category, query);
            try {
                prepared = storage.prepareStatement(desc);
                agentCursor = prepared.executeQuery();
            } catch (DescriptorParsingException e) {
                logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
                return Collections.emptySet();
            } catch (StatementExecutionException e) {
                logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
                return Collections.emptySet();
            }
            while (agentCursor.hasNext()) {
                agents.add(agentCursor.next().getAgentId());
            }
        }
        return agents;
    }

}
