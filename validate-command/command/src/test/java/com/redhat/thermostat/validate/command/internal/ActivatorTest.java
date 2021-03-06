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

package com.redhat.thermostat.validate.command.internal;

import static com.redhat.thermostat.testutils.Asserts.assertCommandIsRegistered;
import static com.redhat.thermostat.testutils.Asserts.assertServiceIsRegistered;
import static org.junit.Assert.*;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.FileNameTabCompleter;
import org.junit.Test;

import com.redhat.thermostat.testutils.StubBundleContext;

import java.util.List;

public class ActivatorTest {

    @Test
    public void testCommandsRegistered() throws Exception {
        StubBundleContext ctx = new StubBundleContext();
        
        Activator activator = new Activator();
        
        activator.start(ctx);

        assertCommandIsRegistered(ctx, "validate", ValidateCommand.class);
        
        activator.stop(ctx);
        
        assertEquals(0, ctx.getAllServices().size());
    }

    @Test
    public void testValidateCompleterBecomesAvailableWhenFileNameCompleterAppears() throws Exception {
        StubBundleContext ctx = new StubBundleContext();
        ctx.registerService(FileNameTabCompleter.class, new StubFileNameTabCompleter(), null);
        Activator activator = new Activator();
        activator.start(ctx);
        assertEquals(3, ctx.getAllServices().size());
        assertServiceIsRegistered(ctx, FileNameTabCompleter.class, StubFileNameTabCompleter.class);
        assertServiceIsRegistered(ctx, CompleterService.class, ValidateCommandCompleterService.class);
        assertServiceIsRegistered(ctx, Command.class, ValidateCommand.class);
    }

    private static class StubFileNameTabCompleter implements FileNameTabCompleter {
        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            return 0;
        }
    }

}

