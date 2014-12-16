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

package com.redhat.thermostat.agent;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import com.redhat.thermostat.annotations.Service;

/**
 * Maintains an RMI registry used for inter-process communication between
 * the Thermostat agent and other helper processes on the same host.
 * 
 * <p>
 * <b> RMI is no longer used by the Thermostat agent. Invoking any of this
 * service's methods will result in a {@link RemoteException}. </b>
 */
@Service
@Deprecated
public interface RMIRegistry {

    /**
     * @return the underlying {@link Registry} maintained by this service.
     * @throws RemoteException if this method fails to obtain the registry
     */
    Registry getRegistry() throws RemoteException;

    /**
     * Exports the provided remote object to the RMI registry.
     * @param obj - the object to be exported
     * @return a remote stub of the exported object
     * @throws RemoteException if this method fails to export the object
     */
    Remote export(Remote obj) throws RemoteException;

    /**
     * Unexports a previously exported remote object from the RMI registry.
     * @param obj - the object to be unexported
     * @return whether the operation was successful
     * @throws RemoteException if an error occurred trying to unexport 
     * the object
     */
    boolean unexport(Remote obj) throws RemoteException;

}

