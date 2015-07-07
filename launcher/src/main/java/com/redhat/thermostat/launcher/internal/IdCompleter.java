/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

public class IdCompleter implements Completer {

    private IdFinder finder;
    private final StorageState storageState;

    public IdCompleter(IdFinder finder, StorageState storageState) {
        this.finder = finder;
        this.storageState = storageState;
    }

    @Override
    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        List<CompletionInfo> ids;
        List<String> completions = new ArrayList<>();

        if (storageState.isStorageConnected()) {
            ids = finder.findIds();
            ids = filterIdsWithBuffer(ids, buffer);

            //If there is only one it will be completed on the prompt line.
            //Then any additional information is unwanted there.
            if (ids.size() == 1) {
                completions = getCompletions(ids);
            } else {
                completions = getCompletionsWithUserVisibleText(ids);
            }
        }

        StringsCompleter idsCompleter = new StringsCompleter(completions);
        return idsCompleter.complete(buffer, cursor, candidates);
    }

    private List<CompletionInfo> filterIdsWithBuffer(final List<CompletionInfo> ids, String buffer) {
        List<CompletionInfo> result = new ArrayList<>();
        for (CompletionInfo id : ids) {
            if (buffer == null || id.getActualCompletion().startsWith(buffer)) {
                result.add(id);
            }
        }
        return result;
    }

    private List<String> getCompletions(final List<CompletionInfo> vmIds) {
        List<String> result = new ArrayList<>();
        for (CompletionInfo vmId : vmIds) {
            result.add(vmId.getActualCompletion());
        }
        return result;
    }

    private List<String> getCompletionsWithUserVisibleText(final List<CompletionInfo> vmIds) {
        List<String> result = new ArrayList<>();
        for (CompletionInfo vmId : vmIds) {
            result.add(vmId.getCompletionWithUserVisibleText());
        }
        return result;
    }

}

