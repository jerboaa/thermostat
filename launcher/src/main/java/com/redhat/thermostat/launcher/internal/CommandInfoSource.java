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

package com.redhat.thermostat.launcher.internal;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class CommandInfoSource {

    private static final Logger logger = Logger.getLogger(CommandInfoSource.class.getSimpleName());
    private Map<String, CommandInfo> commands;

    CommandInfoSource(String thermostatHome) {
        commands = new HashMap<>();
        final File dir = new File(thermostatHome + File.separator + "etc", "commands");
        if (dir.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File theDir, String filename) {
                    if (!theDir.equals(dir)) {
                        return false;
                    }
                    return filename.endsWith(".properties");
                }

            };
            File[] commandPropertyFiles = dir.listFiles(filter);
            for (File file : commandPropertyFiles) {
                Properties commandProps = new Properties();
                try {
                    commandProps.load(new FileReader(file));
                } catch (IOException ignore) {
                    // This means the command won't work, if it has dependencies it
                    // needs to load.  Also, it will not appear in help listing.
                    logger.warning("Issue loading properties file: " + file.getPath());
                }
                String commandName = deduceCommandName(file.getName());
                commands.put(commandName, new CommandInfo(commandName, commandProps, thermostatHome));
            }
        } else {
            logger.warning("Command configuration directory not found or not a directory: " + dir.getPath());
        }
    }

    private String deduceCommandName(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return fileName.substring(0, dotIndex);
    }

    public CommandInfo getCommandInfo(String name) {
        return commands.get(name);
    }

    public Collection<CommandInfo> getCommandInfos() {
        return commands.values();
    }

}
