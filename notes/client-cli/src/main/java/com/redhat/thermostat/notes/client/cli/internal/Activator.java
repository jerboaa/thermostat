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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class Activator implements BundleActivator {

    private ServiceRegistration commandRegistration;
    private ServiceRegistration completerServiceRegistration;
    private MultipleServiceTracker serviceTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        final ListNotesSubcommand listNotesSubcommand = new ListNotesSubcommand();
        final AddNoteSubcommand addNoteSubcommand = new AddNoteSubcommand();
        final DeleteNoteSubcommand deleteNoteSubcommand = new DeleteNoteSubcommand();
        final UpdateNoteSubcommand updateNoteSubcommand = new UpdateNoteSubcommand();

        final NoteIdsFinder noteIdsFinder = new NoteIdsFinder();

        final NotesControlCommand controlCommand = new NotesControlCommand(noteIdsFinder, addNoteSubcommand,
                deleteNoteSubcommand, updateNoteSubcommand, listNotesSubcommand);

        Class<?>[] serviceDeps = new Class<?>[]{
                VmInfoDAO.class,
                HostInfoDAO.class,
                AgentInfoDAO.class,
                VmNoteDAO.class,
                HostNoteDAO.class,
        };

        serviceTracker = new MultipleServiceTracker(context, serviceDeps, new MultipleServiceTracker.Action() {

            private final List<NotesSubcommand> subcommands =
                    Arrays.asList(listNotesSubcommand, addNoteSubcommand, deleteNoteSubcommand, updateNoteSubcommand);

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                VmInfoDAO vmInfoDAO = services.get(VmInfoDAO.class);
                AgentInfoDAO agentInfoDAO = services.get(AgentInfoDAO.class);
                HostInfoDAO hostInfoDAO = services.get(HostInfoDAO.class);
                VmNoteDAO vmNoteDAO = services.get(VmNoteDAO.class);
                HostNoteDAO hostNoteDAO = services.get(HostNoteDAO.class);

                for (NotesSubcommand subcommand : subcommands) {
                    subcommand.bindVmInfoDao(vmInfoDAO);
                    subcommand.bindHostInfoDao(hostInfoDAO);
                    subcommand.bindAgentInfoDao(agentInfoDAO);
                    subcommand.bindVmNoteDao(vmNoteDAO);
                    subcommand.bindHostNoteDao(hostNoteDAO);
                }

                noteIdsFinder.bindVmNoteDao(vmNoteDAO);
                noteIdsFinder.bindHostNoteDao(hostNoteDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                noteIdsFinder.servicesUnavailable();
                for (NotesSubcommand command : subcommands) {
                    command.servicesUnavailable();
                }
            }
        });
        serviceTracker.open();

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Command.NAME, NotesControlCommand.COMMAND_NAME);
        commandRegistration = context.registerService(Command.class.getName(), controlCommand, properties);
        completerServiceRegistration = context.registerService(CompleterService.class, controlCommand, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
        if (commandRegistration != null) {
            commandRegistration.unregister();
        }
        if (completerServiceRegistration != null) {
            completerServiceRegistration.unregister();
        }
    }

}
