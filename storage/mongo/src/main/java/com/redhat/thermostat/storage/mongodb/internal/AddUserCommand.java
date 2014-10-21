/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.storage.mongodb.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.StorageAuthInfoGetter;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.QueuedStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;

/**
 * This command needs to be in the mongodb storage bundle since it
 * uses MongoStorage directly (casts to it).
 *
 */
public class AddUserCommand extends BaseAddUserCommand {
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    static final String DB_URL_ARG = "dbUrl";
    private final BundleContext context;
    private final StorageCredentials emptyCredentials;
    
    AddUserCommand(BundleContext context) {
        this.context = context;
        // These are empty credentials we'll use for the initial connect. We
        // connect with them when the local host exception is turned off.
        emptyCredentials = new StorageCredentials() {
            
            @Override
            public String getUsername() {
                return null;
            }
            
            @Override
            public char[] getPassword() {
                return null;
            }
        };
    }

    // PRE: storage started with --permitLocalHostException.
    // FIXME: Is there anything we can do to ensure this precondition?
    @Override
    public void run(CommandContext ctx) throws CommandException {
        // Check if mongodb stamp file exists.
        ServiceReference commonPathRef = context.getServiceReference(CommonPaths.class.getName());
        requireNonNull(commonPathRef, t.localize(LocaleResources.COMMON_PATHS_SERVICE_UNAVAILABLE));
        CommonPaths commonPath = (CommonPaths)context.getService(commonPathRef);
        File dataDir = commonPath.getUserPersistentDataDirectory();
        // Since this is backing storage specific, it's most likely not a good
        // candidate for CommonPaths
        File mongodbSetupStamp = new File(dataDir, BaseAddUserCommand.MONGODB_STAMP_FILE_NAME);
        if (mongodbSetupStamp.exists()) {
            String msg = t.localize(LocaleResources.MONGODB_SETUP_FILE_EXISTS,
                                    mongodbSetupStamp.getAbsolutePath()).getContents();
            ctx.getConsole().getOutput().println(msg);
            return;
        }
        
        ServiceReference dbServiceRef = context.getServiceReference(DbService.class);
        if (dbServiceRef != null) {
            // Already connected, bail out
            throw new CommandException(t.localize(LocaleResources.ALREADY_CONNECTED_TO_STORAGE_WARNING));
        }
        String dbUrl = ctx.getArguments().getArgument(DB_URL_ARG);
        // dbUrl is a required argument. This should never happen
        Objects.requireNonNull(dbUrl);
        // we only understand "mongodb://" URLs
        if (!dbUrl.startsWith("mongodb://")) {
            throw new CommandException(t.localize(LocaleResources.UNKNOWN_STORAGE_URL));
        }
        
        // Register empty credentials so that connection succeeds
        ServiceRegistration reg = context.registerService(StorageCredentials.class.getName(), emptyCredentials, null);
        DbServiceFactory factory = new DbServiceFactory();
        DbService service = factory.createDbService(dbUrl);
        // this synchronously connects to storage
        service.connect();
        // Unregister empty credentials
        reg.unregister();
        
        ServiceReference storageRef = context.getServiceReference(Storage.class.getName());
        requireNonNull(storageRef, t.localize(LocaleResources.STORAGE_SERVICE_UNAVAILABLE));
        @SuppressWarnings("unchecked")
        // FIXME: Hack alarm. We use knowledge that via MongoStorageProvider we
        //        have a MongoStorage instance wrapped in QueuedStorage. What's
        //        more, we use the "delegate" field name in order to get at the
        //        MongoStorage instance, which we cast to. I'm not sure if adding
        //        method in BackingStorage (the interface) directly would be 
        //        any better than this. After all, this is very backing storage
        //        impl specific. For now do this hack :-/  
        QueuedStorage storage = (QueuedStorage)context.getService(storageRef);
        MongoStorage mongoStorage = getDelegate(storage);
        requireNonNull(mongoStorage, t.localize(LocaleResources.MONGOSTORAGE_RETRIEVAL_FAILED));
        StorageAuthInfoGetter getter = null;
        try {
            LocalizedString userPrompt = new LocalizedString("Please enter the username you'd like to add to mongodb storage at " + dbUrl + ": ");
            LocalizedString passWordPrompt = new LocalizedString("Please enter the desired password of this user: ");
            getter = new StorageAuthInfoGetter(ctx.getConsole(), userPrompt, passWordPrompt);
        } catch (IOException e) {
            throw new CommandException(t.localize(LocaleResources.ADDING_USER_FAILED));
        }
        ConsoleStorageCredentials creds = new ConsoleStorageCredentials(getter);
        addUser(mongoStorage, creds);
        
        // create the STAMP file
        try {
            mongodbSetupStamp.createNewFile();
        } catch (IOException e) {
            String msg = t.localize(LocaleResources.STAMP_FILE_CREATION_FAILED,
                                    mongodbSetupStamp.getAbsolutePath()).getContents();
            ctx.getConsole().getError().println(msg);
            throw new CommandException(new LocalizedString(msg));
        }
        
        String msg = t.localize(LocaleResources.MONGODB_USER_SETUP_COMPLETE).getContents();
        ctx.getConsole().getOutput().println(msg);
    }
    
    // package-private for testing
    void addUser(MongoStorage mongoStorage, StorageCredentials creds) {
        // It's important that getUsername is called prior getPassword.
        // otherwise prompts don't make much sense.
        String username = creds.getUsername();
        char[] pwd = creds.getPassword();
        mongoStorage.addUser(username, pwd);
        // zero out password. We no longer use it.
        Arrays.fill(pwd, '\0');
    }

    // package-private for testing
    MongoStorage getDelegate(QueuedStorage storage) {
        try {
            Field field = storage.getClass().getDeclaredField("delegate");
            field.setAccessible(true);
            MongoStorage mongo = (MongoStorage)field.get(storage);
            return mongo;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public boolean isStorageRequired() {
        return false;
    }

}
