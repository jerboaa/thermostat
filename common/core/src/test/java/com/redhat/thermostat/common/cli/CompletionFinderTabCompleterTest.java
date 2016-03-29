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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompletionFinderTabCompleterTest {

    private CompletionFinder finder;
    private TabCompleter completer;

    @Before
    public void setup() {
        finder = mock(CompletionFinder.class);
        completer = new CompletionFinderTabCompleter(finder);
    }

    @Test
    public void testNoCandidates() {
        List<CharSequence> candidates = new ArrayList<>();

        when(finder.findCompletions()).thenReturn(Collections.<CompletionInfo>emptyList());

        completer.complete(null, 0, candidates);

        verify(finder).findCompletions();

        assertThat(candidates, is(equalTo(Collections.<CharSequence>emptyList())));
    }

    @Test
    public void testOnlyOneCandidate() {
        List<CharSequence> candidates = new ArrayList<>();

        CompletionInfo completionInfo = new CompletionInfo("actual-completion", "user-text");
        when(finder.findCompletions()).thenReturn(Collections.singletonList(completionInfo));

        completer.complete(null, 0, candidates);

        verify(finder).findCompletions();

        assertThat(candidates, is(equalTo(Collections.singletonList((CharSequence) completionInfo.getActualCompletion()))));
    }

    @Test
    public void testMultipleCandidates() {
        List<CharSequence> candidates = new ArrayList<>();

        CompletionInfo info1 = new CompletionInfo("actual-completion1", "user-text1");
        CompletionInfo info2 = new CompletionInfo("actual-completion2", "user-text2");
        when(finder.findCompletions()).thenReturn(Arrays.asList(info1, info2));

        completer.complete(null, 0, candidates);

        List<String> stringCandidates = new ArrayList<>();
        for (CharSequence cs : candidates) {
            stringCandidates.add(cs.toString());
        }

        verify(finder).findCompletions();

        assertThat(stringCandidates,
                is(equalTo(Arrays.asList(info1.getCompletionWithUserVisibleText(), info2.getCompletionWithUserVisibleText()))));
    }

}
