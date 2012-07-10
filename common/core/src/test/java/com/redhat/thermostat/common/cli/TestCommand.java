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

package com.redhat.thermostat.common.cli;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


class TestCommand implements Command {

    private String name;
    private Handle handle;
    private String description;
    private String usage;
    private boolean storageRequired;
    private boolean availableInShell = true;
    private boolean availableOutsideShell = true;

    private List<ArgumentSpec> arguments = new LinkedList<ArgumentSpec>();

    static interface Handle {
        public void run(CommandContext ctx) throws CommandException;
        public void stop();
    }

    TestCommand(String name) {
        this(name, null);
    }

    TestCommand(String name, Handle r) {
        this.name = name;
        this.handle = r;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        if (handle != null) {
            handle.run(ctx);
        }
    }

    @Override
    public void enable() {
        // TODO what do we do here?
    }

    @Override
    public void disable() {
        if (handle != null) {
            handle.stop();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    void setDescription(String desc) {
        description = desc;
    }

    @Override
    public String getUsage() {
        return usage;
    }

    void setUsage(String usage) {
        this.usage = usage;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        return arguments;
    }

    void addArguments(ArgumentSpec... arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
    }

    @Override
    public boolean isStorageRequired() {
        return storageRequired;
    }

    void setStorageRequired(boolean storageRequired) {
        this.storageRequired = storageRequired;
    }

    @Override
    public boolean isAvailableInShell() {
        return availableInShell;
    }

    void setAvailableInShell(boolean avaiable) {
        this.availableInShell = avaiable;
    }

    @Override
    public boolean isAvailableOutsideShell() {
        return availableOutsideShell;
    }

    void setAvailableOutsideShell(boolean available) {
        this.availableOutsideShell = available;
    }
}
