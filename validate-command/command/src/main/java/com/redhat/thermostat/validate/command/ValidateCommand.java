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

package com.redhat.thermostat.validate.command;

import java.io.File;
import java.io.FileNotFoundException;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.plugin.validator.PluginConfigurationValidatorException;
import com.redhat.thermostat.plugin.validator.PluginValidator;
import com.redhat.thermostat.plugin.validator.ValidationErrorsFormatter;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.validate.command.locale.LocaleResources;

public class ValidateCommand implements Command {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private PluginValidator validator;

    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        validator = new PluginValidator();
        File pluginFile = null;
        String argString = null;
        
            try {
                argString = args.getNonOptionArguments().get(0);
                pluginFile = new File(argString);
                validator.validate(pluginFile);
                ctx.getConsole().getOutput().println(translator.localize(
                                LocaleResources.VALIDATION_SUCCESSFUL, pluginFile.getAbsolutePath())
                                .getContents());
                
            } catch (PluginConfigurationValidatorException e) {
                ValidationErrorsFormatter formatter = new ValidationErrorsFormatter();
                ctx.getConsole().getError().println(formatter.format(e.getAllErrors()));
                ctx.getConsole().getError().println(translator.localize(
                                LocaleResources.VALIDATION_FAILED, pluginFile.getAbsolutePath())
                               .getContents());
                
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                throw new CommandLineArgumentParseException
                (translator.localize(
                        LocaleResources.FILE_REQUIRED));
                
            } catch (FileNotFoundException fnfe) {
                throw new CommandLineArgumentParseException
                (translator.localize(
                        LocaleResources.FILE_NOT_FOUND, pluginFile.getAbsolutePath()));
            }        
    }

    public boolean isStorageRequired() {
        return false;
    }

}

