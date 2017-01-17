/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.populator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.common.cli.AbstractCompleterCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CompletionFinderTabCompleter;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.populator.internal.StoragePopulatorConfigFinder;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.config.PopulationConfig;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;
import com.redhat.thermostat.storage.populator.internal.AgentInfoPopulator;
import com.redhat.thermostat.storage.populator.internal.CollectionPopulator;
import com.redhat.thermostat.storage.populator.internal.HostInfoPopulator;
import com.redhat.thermostat.storage.populator.internal.NetworkInfoPopulator;
import com.redhat.thermostat.storage.populator.internal.ThreadPopulator;
import com.redhat.thermostat.storage.populator.internal.VmInfoPopulator;
import com.redhat.thermostat.storage.populator.internal.LocaleResources;
import com.redhat.thermostat.thread.dao.ThreadDao;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

@Component
@Service // Command and CompleterService
@Property(name = Command.NAME, value = "storage-populator")
public class StoragePopulatorCommand extends AbstractCompleterCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    static final CliCommandOption CONFIG_OPTION = new CliCommandOption("c", "config", true, "the json config file to use", true);

    private final Map<String, CollectionPopulator> populators = new HashMap<>();
    private final DependencyServices dependencyServices = new DependencyServices();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private CommonPaths paths;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private HostInfoDAO hostInfoDAO;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private AgentInfoDAO agentInfoDAO;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private VmInfoDAO vmInfoDAO;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private NetworkInterfaceInfoDAO networkInfoDAO;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ThreadDao threadDAO;

    private Console console;

    public void bindPaths(CommonPaths paths) {
        dependencyServices.addService(CommonPaths.class, paths);
    }

    public void unbindPaths(CommonPaths paths) {
        dependencyServices.removeService(CommonPaths.class);
    }

    public void bindHostInfoDAO(HostInfoDAO dao) {
        dependencyServices.addService(HostInfoDAO.class, dao);
    }

    public void unbindHostInfoDAO(HostInfoDAO dao) {
        dependencyServices.removeService(HostInfoDAO.class);
    }

    public void bindAgentInfoDAO(AgentInfoDAO dao) {
        dependencyServices.addService(AgentInfoDAO.class, dao);
    }

    public void unbindAgentInfoDAO(AgentInfoDAO dao) {
        dependencyServices.removeService(AgentInfoDAO.class);
    }

    public void bindVmInfoDAO(VmInfoDAO dao) {
        dependencyServices.addService(VmInfoDAO.class, dao);
    }

    public void unbindVmInfoDAO(VmInfoDAO dao) {
        dependencyServices.removeService(VmInfoDAO.class);
    }

    public void bindNetworkInfoDAO(NetworkInterfaceInfoDAO dao) {
        dependencyServices.addService(NetworkInterfaceInfoDAO.class, dao);
    }

    public void unbindNetworkInfoDAO(NetworkInterfaceInfoDAO dao) {
        dependencyServices.removeService(NetworkInterfaceInfoDAO.class);
    }

    public void bindThreadDAO(ThreadDao dao) {
        dependencyServices.addService(ThreadDao.class, dao);
    }

    public void unbindThreadDAO(ThreadDao dao) {
        dependencyServices.removeService(ThreadDao.class);
    }

    @Override
    public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
        CompletionFinderTabCompleter completer = new CompletionFinderTabCompleter(new StoragePopulatorConfigFinder(dependencyServices));
        return Collections.singletonMap(CONFIG_OPTION, completer);
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        console = ctx.getConsole();

        paths = dependencyServices.getRequiredService(CommonPaths.class);
        hostInfoDAO = dependencyServices.getRequiredService(HostInfoDAO.class);
        agentInfoDAO = dependencyServices.getRequiredService(AgentInfoDAO.class);
        vmInfoDAO = dependencyServices.getRequiredService(VmInfoDAO.class);
        networkInfoDAO = dependencyServices.getRequiredService(NetworkInterfaceInfoDAO.class);
        threadDAO = dependencyServices.getRequiredService(ThreadDao.class);

        HostInfoPopulator hostInfoPopulator = new HostInfoPopulator(hostInfoDAO);
        populators.put(hostInfoPopulator.getHandledCollection(), hostInfoPopulator);

        AgentInfoPopulator agentInfoPopulator = new AgentInfoPopulator(agentInfoDAO);
        populators.put(agentInfoPopulator.getHandledCollection(), agentInfoPopulator);

        VmInfoPopulator vmInfoPopulator = new VmInfoPopulator(vmInfoDAO);
        populators.put(vmInfoPopulator.getHandledCollection(), vmInfoPopulator);

        NetworkInfoPopulator networkInfoPopulator = new NetworkInfoPopulator(networkInfoDAO);
        populators.put(networkInfoPopulator.getHandledCollection(), networkInfoPopulator);

        ThreadPopulator threadPopulator = new ThreadPopulator(threadDAO);
        populators.put(threadPopulator.getHandledCollection(), threadPopulator);

        try {
            PopulationConfig config = getPopulationConfigFromArg(ctx.getArguments());
            addItemsToStorage(config);
        } catch (IllegalArgumentException e) {
            console.getError().println(e.getLocalizedMessage());
        }
    }

    private PopulationConfig getPopulationConfigFromArg(Arguments args) {
        File configFile = getConfigFile(args);
        if (!configFile.exists()) {
            throw new IllegalArgumentException(translator.localize(
                    LocaleResources.NONEXISTENT_CONFIG, configFile.getAbsolutePath()).getContents());
        }
        String json;
        PopulationConfig config;
        try {
            json = new String(Files.readAllBytes(configFile.toPath()));
            config = PopulationConfig.parseFromJsonString(json);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    translator.localize(LocaleResources.CONFIG_PARSING_FAILED).getContents(), e);
        }

        return config;
    }

    /**
     * Package-private to allow overriding for testing.
     */
    File getConfigFile(Arguments args) {
        return new File(getConfigFileDirectoryPath(paths) + args.getArgument(CONFIG_OPTION.getLongOpt()));
    }

    public static String getConfigFileDirectoryPath(CommonPaths paths) {
        return paths.getSystemPluginConfigurationDirectory().getAbsolutePath() +
                "/storage-populator/";
    }

    /**
     * Add items to storage in topologically sorted order so as to have
     * relevant depending state available.
     *
     * @param config The configuration describing the records to be added
     */
    private void addItemsToStorage(PopulationConfig config) {
        SharedState state = new SharedState();
        for (ConfigItem item: config.getConfigsTopologicallySorted()) {
            state = addRecordsForConfig(item, state);
        }
    }

    private SharedState addRecordsForConfig(ConfigItem item, SharedState state) {
        CollectionPopulator populator = populators.get(item.getName());
        if (populator == null) {
            throw new IllegalArgumentException(translator.localize(
                    LocaleResources.NO_POPULATOR_FOUND, item.getName()).getContents());
        } else {
            return populator.addPojos(item, state, console);
        }
    }
}