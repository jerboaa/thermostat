/*
 * Copyright 2013 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoNotFoundException;
import com.redhat.thermostat.common.cli.CommandInfoSource;

/**
 * Presents multiple {@link CommandInfoSource}s as one
 * <p>
 * Unfortunately, it can't just delegate requests; it has to merge them.
 */
public class CompoundCommandInfoSource implements CommandInfoSource {

    private final CommandInfoSource source1;
    private final CommandInfoSource source2;

    public CompoundCommandInfoSource(CommandInfoSource source1, CommandInfoSource source2) {
        this.source1 = Objects.requireNonNull(source1);
        this.source2 = Objects.requireNonNull(source2);
    }

    @Override
    public CommandInfo getCommandInfo(String name) throws CommandInfoNotFoundException {
        CommandInfo info1;
        CommandInfo info2;

        try {
            info1 = source1.getCommandInfo(name);
        } catch (CommandInfoNotFoundException notFound) {
            info1 = null;
        }
        try {
            info2 = source2.getCommandInfo(name);
        } catch (CommandInfoNotFoundException notFound) {
            info2 = null;
        }

        if (info1 == null && info2 == null) {
            throw new CommandInfoNotFoundException(name);
        }

        if (info1 == null) {
            return info2;
        } if (info2 == null) {
            return info1;
        } else {
            return merge(info1, info2);
        }
    }

    @Override
    public Collection<CommandInfo> getCommandInfos() {
        return mergeAll(source1.getCommandInfos(), source2.getCommandInfos());
    }

    private Collection<CommandInfo> mergeAll(Collection<CommandInfo> commandInfos1, Collection<CommandInfo> commandInfos2) {
        Map<String, CommandInfo> result = new HashMap<>();
        for (CommandInfo info : commandInfos1) {
            result.put(info.getName(), info);
        }
        for (CommandInfo info : commandInfos2) {
            String cmdName = info.getName();
            if (!result.containsKey(cmdName)) {
                result.put(cmdName, info);
            } else {
                result.put(cmdName, merge(result.get(cmdName), info));
            }
        }

        return result.values();
    }

    private CommandInfo merge(CommandInfo info1, CommandInfo info2) {
        if (!info1.getName().equals(info2.getName())) {
            throw new IllegalArgumentException("command information have different names");
        }
        String name = info1.getName();

        String description = selectBest(info1.getDescription(), info2.getDescription());
        String usage = selectBest(info1.getUsage(), info2.getUsage());
        Options options = selectBest(info1.getOptions(), info2.getOptions());
        List<String> resources = new ArrayList<>();
        resources.addAll(info1.getDependencyResourceNames());
        resources.addAll(info2.getDependencyResourceNames());

        return new BasicCommandInfo(name, description, usage, options, resources);
    }

    private <T> T selectBest(T first, T second) {
        T result;
        if (Objects.equals(first, second)) {
            result = first;
        } else if (first == null) {
            result = second;
        } else if (second == null) {
            result = first;
        } else {
            throw new IllegalArgumentException("two conflicting values!");
        }
        return result;
    }

}
