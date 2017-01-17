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

package com.redhat.thermostat.client.cli.internal;

import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.launcher.InteractiveStorageCredentials;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.utils.keyring.Keyring;

/**
 * Command in order to persistently connect to a database. Available only in
 * shell.
 * 
 * This commands registers a connection service. If this service is available,
 * it can be used to retrieve a DB connection.
 * 
 */
public class ConnectCommand extends AbstractCommand {

    private static final String DB_URL_ARG = "dbUrl";
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final ClientPreferences prefs;
    private final Keyring keyring;
    private final SSLConfiguration sslConf;
    private BundleContext context;
    private DbServiceFactory dbServiceFactory;

    public ConnectCommand(BundleContext context, ClientPreferences prefs, Keyring keyring, SSLConfiguration sslConf) {
        this(context, new DbServiceFactory(), prefs, keyring, sslConf);
    }
    
    ConnectCommand(BundleContext context, DbServiceFactory dbServiceFactory, ClientPreferences prefs, Keyring keyring, SSLConfiguration sslConf) {
        this.context = context;
        this.dbServiceFactory = dbServiceFactory;
        this.prefs = prefs;
        this.keyring = keyring;
        this.sslConf = sslConf;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference<DbService> dbServiceRef = context.getServiceReference(DbService.class);
        if (dbServiceRef != null) {
            DbService service = context.getService(dbServiceRef);
            String connectionUrl = service.getConnectionUrl();
            context.ungetService(dbServiceRef);
            // Already connected, bail out
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_ALREADY_CONNECTED, connectionUrl));
        }
        String dbUrl = ctx.getArguments().getArgument(DB_URL_ARG);
        // This argument is considered "required" so option parsing should mean this is impossible.
        Objects.requireNonNull(dbUrl);
        StorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, dbUrl, ctx.getConsole());

        try {
            // may throw StorageException if storage url is not supported
            DbService service = dbServiceFactory.createDbService(dbUrl, creds, sslConf);
            service.connect();
        } catch (StorageException ex) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_INVALID_STORAGE, dbUrl));
        } catch (ConnectionException ex) {
            String error = ex.getMessage();
            String message = ( error == null ? "" : " " + translator.localize(LocaleResources.COMMAND_CONNECT_ERROR, error).getContents() );
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_FAILED_TO_CONNECT, dbUrl + message), ex);
        }
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }
}

