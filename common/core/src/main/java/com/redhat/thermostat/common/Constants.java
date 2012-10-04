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

package com.redhat.thermostat.common;

/**
 * A grab bag of constants. This could be cleaned up later, but lets throw
 * things here for now.
 */
public class Constants {

    public static final int EXIT_UNKNOWN_ERROR = 1;
    public static final int EXIT_UNABLE_TO_CONNECT_TO_DATABASE = 2;
    public static final int EXIT_UNABLE_TO_READ_PROPERTIES = 3;
    public static final int EXIT_CONFIGURATION_ERROR = 4;
    public static final int EXIT_BACKEND_LOAD_ERROR = 5;
    public static final int EXIT_BACKEND_START_ERROR = 6;

    public static final int SAMPLING_INTERVAL_UNKNOWN = -1;

    public static final String AGENT_LOCAL_HOSTNAME = "localhost";

    public static final long KILOBYTES_TO_BYTES = 1000;

    public static final int HOST_INFO_NETWORK_IPV4_INDEX = 0;
    public static final int HOST_INFO_NETWORK_IPV6_INDEX = 1;


}
