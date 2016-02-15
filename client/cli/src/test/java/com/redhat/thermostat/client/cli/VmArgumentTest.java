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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.storage.core.VmId;

public class VmArgumentTest {
    
    private VmId vmId;

    @Before
    public void setup() {
        vmId = new VmId("vmId");
    }

    @Test
    public void testValidRequiredVmId() throws CommandException {
        Arguments args = setupArguments();
        VmArgument vmArgument = VmArgument.required(args);
        assertEquals(vmId, vmArgument.getVmId());
    }

    @Test
    public void testValidNotRequiredVmId() throws CommandException {
        Arguments args = setupArguments();
        VmArgument vmArgument = VmArgument.optional(args);
        assertEquals(vmId, vmArgument.getVmId());
    }

    private Arguments setupArguments () {
        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, vmId.get());
        return args;
    }

    @Test
    public void testNoVmIdButRequired() {
        SimpleArguments args = new SimpleArguments();

        try {
            VmArgument vmArgument = VmArgument.required(args);
            fail("A CommandException should have been thrown.");
        } catch (CommandException e) {
            assertEquals("A vmId is required", e.getMessage());
        }
    }

    @Test
    public void testNoVmIdButNotRequired() {
        SimpleArguments args = new SimpleArguments();

        try {
            VmArgument vmArgument = VmArgument.optional(args);
        } catch (CommandException e) {
            fail("A CommandException should not have been thrown.");
        }
    }
}
