/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileNameArgumentTest {

    private static final String FILENAME = "filename";
    private static final String PATH = "path/to/file";
    private Arguments args;

    @Before
    public void setup() {
        args = mock(Arguments.class);
    }

    @Test(expected = CommandException.class)
    public void testRequiredArgumentNotPresent() throws CommandException {
        when(args.hasArgument(FILENAME)).thenReturn(false);
        when(args.getArgument(FILENAME)).thenReturn(null);
        FileNameArgument.required(args);
    }

    @Test
    public void testRequiredArgumentPresent() throws CommandException {
        when(args.hasArgument(FILENAME)).thenReturn(true);
        when(args.getArgument(FILENAME)).thenReturn(PATH);
        FileNameArgument argument = FileNameArgument.required(args);
        assertThat(argument.isPresent(), is(true));
        assertThat(argument.getFileName(), is(PATH));
    }

    @Test
    public void testOptionalArgumentNotPresent() throws CommandException {
        when(args.hasArgument(FILENAME)).thenReturn(false);
        when(args.getArgument(FILENAME)).thenReturn(null);
        FileNameArgument argument = FileNameArgument.optional(args);
        assertThat(argument.isPresent(), is(false));
        assertThat(argument.getFileName(), is(equalTo(null)));
    }

    @Test
    public void testOptionalArgumentPresent() throws CommandException {
        when(args.hasArgument(FILENAME)).thenReturn(true);
        when(args.getArgument(FILENAME)).thenReturn(PATH);
        FileNameArgument argument = FileNameArgument.optional(args);
        assertThat(argument.isPresent(), is(true));
        assertThat(argument.getFileName(), is(PATH));
    }

}
