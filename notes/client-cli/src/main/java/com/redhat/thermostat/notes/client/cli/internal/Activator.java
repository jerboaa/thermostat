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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Activator implements BundleActivator {

    private CommandRegistry reg;
    private MultipleServiceTracker serviceTracker;
    private MultipleServiceTracker noteIdCompleterDepsTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        reg = new CommandRegistryImpl(context);

        ListNotesCommand listNotesCommand = new ListNotesCommand();
        registerCommand(ListNotesCommand.NAME, listNotesCommand);
        AddNoteCommand addNoteCommand = new AddNoteCommand();
        registerCommand(AddNoteCommand.NAME, addNoteCommand);
        DeleteNoteCommand deleteNoteCommand = new DeleteNoteCommand();
        registerCommand(DeleteNoteCommand.NAME, deleteNoteCommand);
        UpdateNoteCommand updateNoteCommand = new UpdateNoteCommand();
        registerCommand(UpdateNoteCommand.NAME, updateNoteCommand);

        final List<AbstractNotesCommand> commands = Arrays.asList(listNotesCommand, addNoteCommand, deleteNoteCommand, updateNoteCommand);

        Class<?>[] serviceDeps = new Class<?>[]{
                VmInfoDAO.class,
                HostInfoDAO.class,
                AgentInfoDAO.class,
                VmNoteDAO.class,
                HostNoteDAO.class,
        };

        serviceTracker = new MultipleServiceTracker(context, serviceDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                VmInfoDAO vmInfoDAO = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                Objects.requireNonNull(vmInfoDAO);
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                Objects.requireNonNull(agentInfoDAO);
                HostInfoDAO hostInfoDAO = (HostInfoDAO) services.get(HostInfoDAO.class.getName());
                Objects.requireNonNull(hostInfoDAO);
                VmNoteDAO vmNoteDAO = (VmNoteDAO) services.get(VmNoteDAO.class.getName());
                Objects.requireNonNull(vmNoteDAO);
                HostNoteDAO hostNoteDAO = (HostNoteDAO) services.get(HostNoteDAO.class.getName());
                Objects.requireNonNull(hostNoteDAO);

                for (NotesCommand command : commands) {
                    command.setVmInfoDao(vmInfoDAO);
                    command.setHostInfoDao(hostInfoDAO);
                    command.setAgentInfoDao(agentInfoDAO);
                    command.setVmNoteDao(vmNoteDAO);
                    command.setHostNoteDao(hostNoteDAO);
                }
            }

            @Override
            public void dependenciesUnavailable() {
                for (NotesCommand command : commands) {
                    command.servicesUnavailable();
                }
            }
        });

        serviceTracker.open();

        final NoteIdCompleterService noteIdCompleterService = new NoteIdCompleterService();
        final Class<?>[] noteIdCompleterDeps = new Class<?>[] { HostNoteDAO.class, VmNoteDAO.class };
        noteIdCompleterDepsTracker = new MultipleServiceTracker(context, noteIdCompleterDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                HostNoteDAO hostNoteDAO = (HostNoteDAO) services.get(HostNoteDAO.class.getName());
                VmNoteDAO vmNoteDAO = (VmNoteDAO) services.get(VmNoteDAO.class.getName());
                noteIdCompleterService.setHostNoteDao(hostNoteDAO);
                noteIdCompleterService.setVmNoteDao(vmNoteDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                noteIdCompleterService.setHostNoteDao(null);
                noteIdCompleterService.setVmNoteDao(null);
            }
        });
        noteIdCompleterDepsTracker.open();

        context.registerService(CompleterService.class, noteIdCompleterService, null);
    }

    private void registerCommand(String name, Command command) {
        reg.registerCommand(name, command);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
        if (noteIdCompleterDepsTracker != null) {
            noteIdCompleterDepsTracker.close();
        }
        if (reg != null) {
            reg.unregisterCommands();
        }
    }

}
