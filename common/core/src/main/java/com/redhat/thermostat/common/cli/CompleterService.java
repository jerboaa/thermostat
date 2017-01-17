/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.annotations.ExtensionPoint;

import java.util.Map;
import java.util.Set;

/**
 * An interface marking a service which provides tab completions for cli/shell commands.
 * For example, a plugin providing a new "foo-command" which has an option "-f/--fooId", can provide a CompleterService
 * implementation so that "-f TAB" can produce a list of possible fooIds. This implementation would return a singleton
 * Set of "foo-command" for {@link CompleterService#getCommands()}, and a Map from a CliCommandOption with short-opt "-f"/
 * long-opt "--fooId" to the relevant {@link TabCompleter} instance for {@link CompleterService#getOptionCompleters()}.
 *
 * There are several built-in completions which Thermostat provides. If your command uses a vmId, then declaring a
 * -v/--vmId option in your thermostat-plugin.xml will give you automagic vmId completions in Thermostat shell.
 * Likewise, -a/--agentId, -d/--dbUrl, and -f/--filename completions are provided if you simply include these options
 * in your XML.
 *
 * @see AbstractCompleterService
 */
@ExtensionPoint
public interface CompleterService {

    /**
     * The set of command names for which this service provides completions. In the shell, these are top-level command
     * names, such as "ping" or "list-vms". In the cli, there are preceded by "thermostat", eg. "thermostat list-vms".
     * @return the set of command names
     */
    Set<String> getCommands();

    /**
     * Provides the mapping of options to corresponding completers.
     * @return the map
     */
    Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters();

    /**
     * Provides the mapping of subcommand options to corresponding completers.
     * The String key is the name of the subcommand; the Map value is a map of options to completers, the same as in
     * {@link #getOptionCompleters()}.
     * @return the map
     */
    Map<String, Map<CliCommandOption, ? extends TabCompleter>> getSubcommandCompleters();

}
