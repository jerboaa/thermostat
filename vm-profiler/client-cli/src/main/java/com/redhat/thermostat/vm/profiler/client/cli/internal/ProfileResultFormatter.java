/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.cli.internal;

import java.io.OutputStream;

import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

public class ProfileResultFormatter {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int NUM_COLUMNS = 3;

    private static final String HEADER_PERCENTAGE = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_PERCENTAGE).getContents();
    private static final String HEADER_TIME = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_TIME).getContents();
    private static final String HEADER_NAME = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_NAME).getContents();

    private TableRenderer renderer = new TableRenderer(NUM_COLUMNS);

    public void addHeader() {
        printLine(HEADER_PERCENTAGE, HEADER_TIME, HEADER_NAME);
    }

    public void addMethodInfo(MethodInfo methodInfo) {
        printLine(String.format("%4f", methodInfo.percentageTime),
                String.valueOf(methodInfo.totalTimeInMillis),
                methodInfo.name);
    }

    public void format(OutputStream output) {
        renderer.render(output);
    }

    private void printLine(String percentage, String time, String name) {
        renderer.printLine(percentage, time, name);
    }
}
