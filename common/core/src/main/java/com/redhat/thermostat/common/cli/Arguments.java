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

package com.redhat.thermostat.common.cli;

import java.util.List;

public interface Arguments {
    @Deprecated
    String HOST_ID_ARGUMENT = "hostId";
    String DB_URL_ARGUMENT = "dbUrl";

    List<String> getNonOptionArguments();
    boolean hasArgument(String name);
    String getArgument(String name);

    /**
     * Get the selected subcommand
     * Subcommands are taken to be the first of any non-option arguments passed to a Command.
     * If any value is returned by this method, that value is guaranteed to be equal to one of the names given for
     * a subcommand defined within the plugin's thermostat-plugin.xml.
     * @return the selected subcommand
     * @throws SubcommandExpectedException if no subcommand was supplied
     * @throws InvalidSubcommandException if an unrecognized subcommand was supplied
     */
    String getSubcommand() throws SubcommandExpectedException, InvalidSubcommandException;
}

