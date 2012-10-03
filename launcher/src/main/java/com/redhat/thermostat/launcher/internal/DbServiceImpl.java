/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.launcher.internal;

import java.util.Objects;

import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.ConnectionException;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.launcher.DbService;

public class DbServiceImpl implements DbService {
    
    private String username;
    private String password;
    private String dbUrl;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration registration;
    
    
    DbServiceImpl(String username, String password, String dbUrl) {
        this.username = username;
        this.password = password;
        this.dbUrl = dbUrl;
    }

    public void connect() throws ConnectionException {
        StartupConfiguration config = new ConnectionConfiguration(dbUrl, username, password);
        
        StorageProvider connProv = new MongoStorageProvider(config);
        DAOFactory daoFactory = new MongoDAOFactory(connProv);
        Connection connection = daoFactory.getConnection();
        connection.connect();
        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        daoFactory.registerDAOsAndStorageAsOSGiServices();
    }
    
    public void disconnect() throws ConnectionException {
        DAOFactory factory = ApplicationContext.getInstance().getDAOFactory();
        Connection connection = factory.getConnection();
        connection.disconnect();
        ApplicationContext.getInstance().setDAOFactory(null);
    }
    
    /**
     * Factory method for creating a DbService instance.
     * 
     * @param username
     * @param password
     * @param dbUrl
     * @return
     */
    public static DbService create(String username, String password, String dbUrl) {
        return new DbServiceImpl(username, password, dbUrl);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ServiceRegistration getServiceRegistration() {
        return registration;
    }

    @Override
    public void setServiceRegistration(@SuppressWarnings("rawtypes") ServiceRegistration registration) {
        this.registration = Objects.requireNonNull(registration);
    }
}
