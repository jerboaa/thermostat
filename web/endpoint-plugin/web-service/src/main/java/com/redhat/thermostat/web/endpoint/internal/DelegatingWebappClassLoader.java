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

package com.redhat.thermostat.web.endpoint.internal;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import com.redhat.thermostat.common.utils.LoggingUtils;

public class DelegatingWebappClassLoader extends WebAppClassLoader {

    private static final boolean DEBUG = false;
    private static final Logger logger = LoggingUtils.getLogger(DelegatingWebappClassLoader.class);
    private final ClassLoader osgiDelegate;

    public DelegatingWebappClassLoader(ClassLoader osgiDelegate, WebAppContext context)
            throws IOException {
        super(context);
        this.osgiDelegate = osgiDelegate;
    }

    private void log(String s) {
        if (DEBUG) {
            logger.log(Level.FINEST, s);
        }
    }

    @Override
    public void addClassPath(String classPath) throws IOException {
        log(String.format("Adding classpath: %s", classPath));
        super.addClassPath(classPath);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            log(String.format("loading class using OSGi delegate: %s", name));
            // try the osgi delegate
            return osgiDelegate.loadClass(name);
        }
    }
}
