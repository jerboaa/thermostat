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

import com.redhat.thermostat.shared.locale.Translate;
import org.apache.felix.scr.annotations.Activate;
import org.osgi.service.component.ComponentContext;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract class to help with implementing commands which also provide tab completions for arguments and sub-
 * commands within Thermostat shell. This is most useful in cases where an argument and its completions are unique to a
 * single command; if your plugin provides multiple commands which have shared options and shared arguments/completions,
 * more flexibility in the implementation can be had by implementing a {@link CompleterService} directly and separately
 * from the Command implementations.
 *
 * This class is meant for use with OSGi Declarative Services. Subclasses should be annotated with @Component, @Service,
 * and @Property(name = Command.NAME, value = "command-name-here") at minimum.
 */
public abstract class AbstractCompleterCommand extends AbstractCommand implements CompleterService {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private String commandName = null;

    /**
     * If implementation subclasses override this method, they <b>must</b> call through to this super implementation as
     * well, or else tab completions will fail to be registered correctly.
     */
    @Activate
    protected void activate(ComponentContext context) {
        commandName = validateCommandName((String) context.getProperties().get(Command.NAME));
    }

    /**
     * The set of command names for which tab completions are provided.
     *
     * Implementations of this class only provide completions for a single command name value.
     * This value is expected to be provided by an @Property annotation defining a {@link Command.NAME} property.
     * @return a singleton set containing the command name
     */
    @Override
    public final Set<String> getCommands() {
        return Collections.singleton(validateCommandName(commandName));
    }

    private String validateCommandName(String name) {
        return Objects.requireNonNull(name, t.localize(LocaleResources.MISSING_COMMAND_NAME, getClass().getName()).getContents());
    }

    @Override
    public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Map<CliCommandOption, ? extends TabCompleter>> getSubcommandCompleters() {
        return Collections.emptyMap();
    }
}
