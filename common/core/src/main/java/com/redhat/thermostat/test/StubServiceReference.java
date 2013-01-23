/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.test;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.test.StubBundleContext.ServiceInformation;

public class StubServiceReference implements ServiceReference {

    private final Bundle sourceBundle;
    private final ServiceInformation information;

    public StubServiceReference(ServiceInformation info, Bundle sourceBundle) {
        this.information = info;
        this.sourceBundle = sourceBundle;
    }

    @Override
    public Object getProperty(String key) {
        return information.properties.get(key);
    }

    @Override
    public String[] getPropertyKeys() {
        Dictionary props = information.properties;
        List<String> toReturn = new ArrayList<>(props.size());
        Enumeration keyEnumeration = props.keys();
        while (keyEnumeration.hasMoreElements()) {
            toReturn.add((String) keyEnumeration.nextElement());
        }
        return toReturn.toArray(new String[0]);
    }

    @Override
    public Bundle getBundle() {
        return sourceBundle;
    }

    @Override
    public Bundle[] getUsingBundles() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        if (sourceBundle == bundle) {
            return true;
        }
        throw new NotImplementedException();
    }

    @Override
    public int compareTo(Object reference) {
        if (!(reference instanceof ServiceReference)) {
            throw new NotImplementedException();
        }

        ServiceReference ref = (ServiceReference) reference;

        Integer myRanking = (Integer) getProperty(Constants.SERVICE_RANKING);
        Integer otherRanking = (Integer) ref.getProperty(Constants.SERVICE_RANKING);

        if (myRanking > otherRanking) {
            return 1;
        } else if (myRanking < otherRanking) {
            return -1;
        } else {
            Integer myServiceId = (Integer) getProperty(Constants.SERVICE_ID);
            Integer otherServiceId = (Integer) ref.getProperty(Constants.SERVICE_ID);

            if (myServiceId < otherServiceId) {
                return 1;
            } else if (myServiceId > otherServiceId) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public ServiceInformation getInformation() {
        return information;
    }

}

