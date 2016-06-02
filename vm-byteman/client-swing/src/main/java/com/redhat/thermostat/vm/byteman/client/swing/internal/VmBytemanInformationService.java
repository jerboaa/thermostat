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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.common.AllPassFilter;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

@Component
@Service(value = InformationService.class)
@Properties({
    @Property(name = "GenericClassName",
              value = "com.redhat.thermostat.storage.core.VmRef"),
    @Property(name = "com.redhat.thermostat.client.core.InformationService.serviceID",
              value = "com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanInformationService")
})
public class VmBytemanInformationService implements InformationService<VmRef> {
    
    @Reference
    private AgentInfoDAO agentInfoDao;
    
    @Reference
    private VmInfoDAO vmInfoDao;
    
    @Reference
    private VmBytemanDAO vmBytemanDao;
    
    @Reference
    private RequestQueue requestQueue;

    @Override
    public int getOrderValue() {
        return Ordered.ORDER_USER_GROUP + 55;
    }

    @Override
    public Filter<VmRef> getFilter() {
        // Showing metrics for dead JVMs - should there be some that got collected -
        // is useful.
        return new AllPassFilter<>();
    }

    @Override
    public InformationServiceController<VmRef> getInformationServiceController(VmRef ref) {
        return new VmBytemanInformationController(ref, agentInfoDao,
                                                  vmInfoDao, vmBytemanDao,
                                                  requestQueue);
    }

}
