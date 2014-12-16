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

package com.redhat.thermostat.agent.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.agent.RMIRegistry;
import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.agent.config.AgentConfigsUtils;
import com.redhat.thermostat.agent.config.AgentStorageCredentials;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.utils.management.internal.AgentProxyFilter;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl;
import com.redhat.thermostat.utils.username.internal.UserNameUtilImpl;

public class Activator implements BundleActivator {
    
    private MXBeanConnectionPoolImpl pool;

    @Override
    public void start(BundleContext context) throws Exception {
        RMIRegistryImpl registry = new RMIRegistryImpl();
        context.registerService(RMIRegistry.class, registry, null);
        ServiceReference<CommonPaths> pathsRef = context.getServiceReference(CommonPaths.class);
        CommonPaths paths = context.getService(pathsRef);
        UserNameUtilImpl usernameUtil = new UserNameUtilImpl();
        context.registerService(UserNameUtil.class, usernameUtil, null);
        pool = new MXBeanConnectionPoolImpl(paths.getSystemBinRoot(), usernameUtil);
        context.registerService(MXBeanConnectionPool.class, pool, null);
        StorageCredentials creds = new AgentStorageCredentials(paths.getUserAgentAuthConfigFile());
        context.registerService(StorageCredentials.class, creds, null);
        AgentConfigsUtils.setConfigFiles(paths.getSystemAgentConfigurationFile(), paths.getUserAgentConfigurationFile());
        paths = null;
        context.ungetService(pathsRef);
        VmBlacklistImpl blacklist = new VmBlacklistImpl();
        blacklist.addVmFilter(new AgentProxyFilter());
        context.registerService(VmBlacklist.class, blacklist, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Services automatically unregistered by framework
        pool = null;
    }

    // Testing hook.
    void setPool(MXBeanConnectionPoolImpl pool) {
        this.pool = pool;
    }

}

