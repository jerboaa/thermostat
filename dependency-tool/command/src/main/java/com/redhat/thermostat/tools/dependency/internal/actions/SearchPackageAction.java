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

package com.redhat.thermostat.tools.dependency.internal.actions;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.tools.dependency.internal.OSGiSearchProcessor;
import com.redhat.thermostat.tools.dependency.internal.PathProcessorHandler;
import com.redhat.thermostat.tools.dependency.internal.Utils;

/**
 */
public class SearchPackageAction {

    private static final String NO_JAR_FOUND = "";

    public static String execute(PathProcessorHandler handler, String target, CommandContext ctx) {

        OSGiSearchProcessor search = new OSGiSearchProcessor(target);
        handler.process(search);
        OSGiSearchProcessor.BundleInfo result = search.getBundleInfo();
        if (result == null) {
            Utils.getInstance().print(ctx, "no library provides package \"" + target + "\"");
            return NO_JAR_FOUND;
        } else {
            Utils.getInstance().printHeader(ctx, "package \"" + target + "\"");
            Utils.getInstance().print(ctx, "provided by: " + result.library);
            Utils.getInstance().print(ctx, "bundle symbolic name: " + result.symbolicName);
            Utils.getInstance().print(ctx, "bundle version: " + result.version);
            return result.library.toString();
        }
    }
}
