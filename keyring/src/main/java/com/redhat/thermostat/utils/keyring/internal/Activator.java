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

package com.redhat.thermostat.utils.keyring.internal;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.OS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.utils.keyring.Keyring;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceRegistration reg;

    private static class DummyKeyringImpl implements Keyring {

        /* Trivial implementation just to keep the world from blowing up.
         * Everything noop.
         */

        @Override
        public void savePassword(String url, String username, char[] password) {
            // NOOP
        }

        @Override
        public char[] getPassword(String url, String username) {
            // NOOP
            return new char[]{};
        }

        @Override
        public void clearPassword(String url, String username) {
            // NOOP
        }
    }

    @Override
    public void start(BundleContext context) throws Exception {
        Keyring theKeyring = null;
        try {
            theKeyring = new KeyringImpl();
        } catch (UnsatisfiedLinkError e) {
            if (OS.IS_UNIX) {
                theKeyring = new DummyKeyringImpl();
            }
            else {
                ServiceTracker<CommonPaths,CommonPaths> pathTracker = new ServiceTracker<CommonPaths,CommonPaths>(context,CommonPaths.class.getName(), null) {
                    @Override
                    public CommonPaths addingService(ServiceReference<CommonPaths> reference) {
                        CommonPaths paths = super.addingService(reference);
                        final Keyring theKeyring = new InsecureFileBasedKeyringImpl(paths);
                        reg = context.registerService(Keyring.class.getName(), theKeyring, null);
                        return paths;
                    }
                    @Override
                    public void removedService(ServiceReference<CommonPaths> reference, CommonPaths service) {
                        if (reg != null) {
                            reg.unregister();
                            reg = null;
                        }
                        super.removedService(reference, service);
                    }
                };
                pathTracker.open();
            }
        }
        if (theKeyring != null)
            context.registerService(Keyring.class.getName(), theKeyring, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Nothing to do
    }
}
