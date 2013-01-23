/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.util.logging.Logger;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.utils.LoggingUtils;

public abstract class CommandWithInfo implements Command {

    private static final Logger logger = LoggingUtils.getLogger(CommandWithInfo.class);
    private CommandInfo info;
    private static final String noDesc = "Description not available.";
    private static final String noUsage = "Usage not available.";

    void setCommandInfo(CommandInfo info) {
        this.info = info; 
    }

    boolean hasCommandInfo() {
        return info != null;
    }

    @Override
    public String getDescription() {
        String desc = null;
        if (hasCommandInfo()) {
            desc = info.getDescription();
        }
        if (desc == null) {
            desc = noDesc;
        }
        return desc;
    }

    @Override
    public String getUsage() {
        String usage = null;
        if (hasCommandInfo()) { 
            usage = info.getUsage();
        }
        if (usage == null) {
            usage = noUsage;
        }
        return usage;
    }

    @Override
    public Options getOptions() {
        try {
            return info.getOptions();
        } catch (NullPointerException e) {
            logger.warning("CommandInfo not yet set, returning empty Options.");
            return new Options();
        }
    }

    @Override
    public boolean isStorageRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAvailableInShell() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAvailableOutsideShell() {
        // TODO Auto-generated method stub
        return false;
    }

}

