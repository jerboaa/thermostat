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

package com.redhat.thermostat.web.server.containers;

import javax.servlet.ServletContext;

/**
 * Factory producing {@link ServletContainerInfo} from strings as returned from
 * {@link ServletContext#getServerInfo()}.
 *
 */
public class ServletContainerInfoFactory {
    
    private static final String JETTY_PREFIX = "jetty";
    private static final String TOMCAT_PREFIX = "Apache Tomcat";
    private static final String WILDFLY_PREFIX = "Undertow";
    private static final String JBOSS_AS_PREFIX = "JBoss Web";
    
    private final String containerInfo;

    public ServletContainerInfoFactory(String containerInfo) {
        this.containerInfo = containerInfo;
    }
    
    /**
     * 
     * @return Information about the hosting container or {@code null} if the
     *         servlet container is not recognized.
     */
    public ServletContainerInfo getInfo() {
        if (containerInfo.startsWith(JETTY_PREFIX)) {
            return new JettyContainerInfo(containerInfo);
        }
        if (containerInfo.startsWith(TOMCAT_PREFIX)) {
            return new TomcatContainerInfo(containerInfo);
        }
        if (containerInfo.startsWith(WILDFLY_PREFIX)
                || containerInfo.startsWith(JBOSS_AS_PREFIX)) {
            return new WildflyContainerInfo(containerInfo);
        }
        // unknown, return null
        return null;
    }
    
}
