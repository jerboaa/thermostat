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

package com.redhat.thermostat.cli;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineArgumentsParser {

    private List<ArgumentSpec> arguments = new LinkedList<>();

    void addArguments(Collection<ArgumentSpec> args) {
        arguments.addAll(args);
    }

    Arguments parse(String[] args) throws CommandLineArgumentParseException {
        try {
            Options options = convertToCommonsCLIOptions(arguments);
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine;
            commandLine = parser.parse(options, args);
            return new CommandLineArguments(commandLine);
        } catch (ParseException e) {
            throw new CommandLineArgumentParseException(e);
        }
    }

    private Options convertToCommonsCLIOptions(Collection<ArgumentSpec> args) {
        Options options = new Options();
        for (ArgumentSpec spec : args) {
            options.addOption(convertSpecToOption(spec));
        }
        return options;
    }

    private Option convertSpecToOption(ArgumentSpec spec) {
        Option option = new Option(spec.getName(), spec.getDescription());
        option.setRequired(spec.isRequired());
        option.setArgs(spec.isUsingAdditionalArgument() ? 1 : 0);
        return option;
    }

    void printHelp(CommandContext ctx, Command cmd) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setOptPrefix("--");
        PrintWriter pw = new PrintWriter(ctx.getConsole().getOutput());
        Options options = convertToCommonsCLIOptions(cmd.getAcceptedArguments());
        helpFormatter.printHelp(pw, 80, cmd.getName(), cmd.getUsage(), options, 2, 4, null, true);
        pw.flush();
    }
}
