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

package com.redhat.thermostat.client.cli;

import com.redhat.thermostat.client.cli.internal.LocaleResources;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;

public class FileNameArgument {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    public static final String ARGUMENT_NAME = "filename";

    private final String fileName;

    private FileNameArgument(Arguments args, boolean isRequired) throws CommandException {
        this.fileName = args.getArgument(ARGUMENT_NAME);
        if (isRequired && fileName == null) {
            throw new CommandException(translator.localize(LocaleResources.FILENAME_REQUIRED));
        }
    }

    public static FileNameArgument required(Arguments args) throws CommandException {
        return new FileNameArgument(args, true);
    }

    public static FileNameArgument optional(Arguments args) throws CommandException {
        return new FileNameArgument(args, false);
    }

    /**
     * @return The value of filename stored in this object, which may be null.
     */
    public String getFileName() {
        return fileName;
    }

    public boolean isPresent() {
        return fileName != null;
    }

}
