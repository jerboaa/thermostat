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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.launcher.BundleInformation;


public class CompoundCommandInfoSourceTest {

    private CommandInfoSource source1;
    private CommandInfoSource source2;
    private CompoundCommandInfoSource compoundSource;

    @Before
    public void setUp() {
        source1 = mock(CommandInfoSource.class);
        source2 = mock(CommandInfoSource.class);

        compoundSource = new CompoundCommandInfoSource(source1, source2);
    }

    @Test(expected = CommandInfoNotFoundException.class)
    public void verifyExceptionThrownOnUnknownCommand() {
        String NAME = "test-command-please-ignore";
        when(source1.getCommandInfo(NAME)).thenThrow(new CommandInfoNotFoundException(NAME));
        when(source2.getCommandInfo(NAME)).thenThrow(new CommandInfoNotFoundException(NAME));

        compoundSource.getCommandInfo(NAME);
    }

    @Test
    public void verifyGetCommandInfoDelegatesToSource1() {
        CommandInfo cmdInfo = mock(CommandInfo.class);
        String NAME = "test-command-please-ignore";
        when(source1.getCommandInfo(NAME)).thenReturn(cmdInfo);
        when(source2.getCommandInfo(NAME)).thenThrow(new CommandInfoNotFoundException(NAME));

        CommandInfo result = compoundSource.getCommandInfo(NAME);
        assertEquals(cmdInfo, result);
    }

    @Test
    public void verifyGetCommandInfoDelegatesToSource2() {
        CommandInfo cmdInfo = mock(CommandInfo.class);
        String NAME = "test-command-please-ignore";
        when(source1.getCommandInfo(NAME)).thenThrow(new CommandInfoNotFoundException(NAME));
        when(source2.getCommandInfo(NAME)).thenReturn(cmdInfo);

        CommandInfo result = compoundSource.getCommandInfo(NAME);
        assertEquals(cmdInfo, result);
    }

    @Test
    public void verifyGetCommandInfoMergesResultFromBothSources() {
        String NAME = "test-command-please-ignore";
        String DESCRIPTION = "test-description";
        String USAGE = "test-usage";
        Options OPTIONS = new Options();
        List<BundleInformation> DEPS1 = Arrays.asList(new BundleInformation("1test1", "1"), new BundleInformation("1test2", "1"));
        List<BundleInformation> DEPS2 = Arrays.asList(new BundleInformation("2test1", "1"));

        CommandInfo cmdInfo1 = mock(CommandInfo.class);
        when(cmdInfo1.getName()).thenReturn(NAME);
        when(cmdInfo1.getDescription()).thenReturn(DESCRIPTION);
        when(cmdInfo1.getUsage()).thenReturn(USAGE);
        when(cmdInfo1.getOptions()).thenReturn(OPTIONS);
        when(cmdInfo1.getBundles()).thenReturn(DEPS1);

        CommandInfo cmdInfo2 = mock(CommandInfo.class);
        when(cmdInfo2.getName()).thenReturn(NAME);
        when(cmdInfo2.getBundles()).thenReturn(DEPS2);

        when(source1.getCommandInfo(NAME)).thenReturn(cmdInfo1);
        when(source2.getCommandInfo(NAME)).thenReturn(cmdInfo2);

        CommandInfo result = compoundSource.getCommandInfo(NAME);
        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(USAGE, result.getUsage());
        assertEquals(OPTIONS, result.getOptions());

        ArrayList<BundleInformation> combined = new ArrayList<>(DEPS1);
        combined.addAll(DEPS2);
        assertEquals(combined, result.getBundles());
    }

    @Test
    public void verifyGetCommandInfosMergesResultsFromBothSources() {
        CommandInfo cmdInfo11 = mock(CommandInfo.class);
        when(cmdInfo11.getName()).thenReturn("cmd1");
        when(cmdInfo11.getDescription()).thenReturn("cmd1");
        when(cmdInfo11.getOptions()).thenReturn(new Options());

        CommandInfo cmdInfo12 = mock(CommandInfo.class);
        when(cmdInfo12.getName()).thenReturn("cmd2");
        when(cmdInfo12.getDescription()).thenReturn("cmd2");
        when(cmdInfo12.getOptions()).thenReturn(new Options());

        when(source1.getCommandInfos()).thenReturn(Arrays.asList(cmdInfo11, cmdInfo12));

        CommandInfo cmdInfo21 = mock(CommandInfo.class);
        when(cmdInfo21.getName()).thenReturn("cmd3");
        when(cmdInfo21.getDescription()).thenReturn("cmd3");
        when(cmdInfo21.getOptions()).thenReturn(new Options());
        CommandInfo cmdInfo22 = mock(CommandInfo.class);
        when(cmdInfo22.getName()).thenReturn("cmd2");

        when(source2.getCommandInfos()).thenReturn(Arrays.asList(cmdInfo21, cmdInfo22));

        Collection<CommandInfo> results = compoundSource.getCommandInfos();
        assertEquals(3, results.size());
    }

    @Test
    public void verifyGetCommandInfosIgnoresIncompleteCommands() {
        CommandInfo cmdInfo11 = mock(CommandInfo.class);
        when(cmdInfo11.getName()).thenReturn("cmd1");
        CommandInfo cmdInfo12 = mock(CommandInfo.class);
        when(cmdInfo12.getName()).thenReturn("cmd2");

        when(source1.getCommandInfos()).thenReturn(Arrays.asList(cmdInfo11, cmdInfo12));

        CommandInfo cmdInfo21 = mock(CommandInfo.class);
        when(cmdInfo21.getName()).thenReturn("cmd3");
        CommandInfo cmdInfo22 = mock(CommandInfo.class);
        when(cmdInfo22.getName()).thenReturn("cmd2");

        when(source2.getCommandInfos()).thenReturn(Arrays.asList(cmdInfo21, cmdInfo22));

        Collection<CommandInfo> results = compoundSource.getCommandInfos();
        assertEquals(0, results.size());
    }
}

