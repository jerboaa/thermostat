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

package com.redhat.thermostat.launcher.internal;

import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class UsageStringBuilder {

    /**
     * Generates a 'usage' string based on the name (of a program) and the
     * options that program will accept on the command line.
     *
     * @param name the program name
     * @param hasSubcommands indicates whether or not this command expects subcommands
     * @param options the options accepted by this program
     * @param positionalArguments a list of positional arguments (those not immediately following an option flag) which
     *                            this command accepts
     * @return a String representing the usage.
     */
    public String getUsage(String name, boolean hasSubcommands, Options options, String... positionalArguments) {
        StringBuilder result = new StringBuilder();
        result.append(name);
        if (hasSubcommands) {
            result.append(" <subcommand>");
        }
        // commons-cli has no support for generics, so suppress this warning.
        @SuppressWarnings("unchecked")
        Collection<Option> opts = options.getOptions();
        // iterate twice to handle/print required options first, followed by optional ones
        for (Option option : opts) {
            appendOption(result, option, true);
        }
        for (Option option : opts) {
            appendOption(result, option, false);
        }
        // print positional arguments last
        if (positionalArguments != null) {
            for (String positionalArg : positionalArguments) {
                result.append(" ").append(positionalArg);
            }
        }

        return result.toString();
    }

    private void appendOption(StringBuilder result, Option option, boolean requiredOptionsOnly) {
        if (option.isRequired() != requiredOptionsOnly) {
            return;
        }

        result.append(" ");

        if (!option.isRequired()) {
            result.append("[");
        }

        // prefer to display long form if available
        if (option.hasLongOpt()) {
            result.append("--").append(option.getLongOpt());
        } else {
            result.append("-").append(option.getOpt());
        }

        if (option.hasArg()) {
            result.append(" ").append("<").append(option.getArgName()).append(">");
        } else if (option.hasOptionalArg()) {
            result.append("[").append(" ").append("<").append(option.getArgName()).append(">").append("]");
        }

        if (!option.isRequired()) {
            result.append("]");
        }
    }

}

