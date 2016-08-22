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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.junit.Test;

import com.redhat.thermostat.launcher.BundleInformation;

public class BasicCommandInfoTest {

    @Test
    public void testBasics() {
        final String NAME = "the_name";
        final String SUMMARY = "some-summary";
        final String DESCRIPTION = "some-description";
        final String USAGE = "some-usage";
        final Options OPTIONS = new Options();
        final List<PluginConfiguration.Subcommand> SUBCOMMANDS = Collections.emptyList();
        final Set<Environment> ENVIRONMENT = EnumSet.noneOf(Environment.class);
        final List<BundleInformation> BUNDLES = Collections.emptyList();

        BasicCommandInfo info = new BasicCommandInfo(NAME, SUMMARY, DESCRIPTION, USAGE, OPTIONS, SUBCOMMANDS, ENVIRONMENT, BUNDLES);

        assertEquals(NAME, info.getName());
        assertEquals(SUMMARY, info.getSummary());
        assertEquals(DESCRIPTION, info.getDescription());
        assertEquals(USAGE, info.getUsage());
        assertEquals(SUBCOMMANDS, info.getSubcommands());
        assertEquals(OPTIONS, info.getOptions());
        assertEquals(BUNDLES, info.getBundles());

        assertEquals(String.format("%s (summary='%s', description='%s', dependencies='%s')", NAME, SUMMARY, DESCRIPTION, BUNDLES.toString()),
                info.toString());
    }
}

