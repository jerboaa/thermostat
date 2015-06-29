/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class AgentIdsFinder implements IdFinder {

    private BundleContext context;

    public AgentIdsFinder(BundleContext context) {
        this.context = context;
    }

    @Override
    public List<String> findIds() {
        List<String> agentIds = new ArrayList<>();
        ServiceReference hostsDAORef = context.getServiceReference(HostInfoDAO.class.getName());
        HostInfoDAO hostsDAO = (HostInfoDAO) context.getService(hostsDAORef);

        Collection<HostRef> hosts = hostsDAO.getHosts();
        context.ungetService(hostsDAORef);

        for (HostRef host : hosts) {
            agentIds.add(host.getAgentId());
        }

        return agentIds;
    }
}
