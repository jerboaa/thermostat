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

package com.redhat.thermostat.tools.dependency;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.tools.dependency.internal.BundleProperties;
import com.redhat.thermostat.tools.dependency.internal.JarLocations;
import com.redhat.thermostat.tools.dependency.internal.PathProcessorHandler;
import com.redhat.thermostat.tools.dependency.internal.actions.ListAllAction;
import com.redhat.thermostat.tools.dependency.internal.actions.ListDependenciesAction;
import com.redhat.thermostat.tools.dependency.internal.actions.PrintOSGIHeaderAction;
import com.redhat.thermostat.tools.dependency.internal.actions.SearchPackageAction;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.redhat.thermostat.tools.dependency.DependencyAnalyzerCommand.NAME;

@Component
@Service(Command.class)
@Property(name = Command.NAME, value = NAME)
public class DependencyAnalyzerCommand implements Command {

    private static class Args {
        public static final String LIST_ALL = "list-all";
        public static final String EXPORTS = "exports";
        public static final String IMPORTS = "imports";
        public static final String INBOUND = "inbound";
        public static final String OUTBOUND = "outbound";
        public static final String PROVIDES = "whatprovides";
    }

    public static final String NAME = "dependency-analyzer";

    private static final Pattern JAR_PATTERN = Pattern.compile(".*\\.jar", Pattern.CASE_INSENSITIVE);

    private static final String NO_JAR_FILE_FOR_PACKAGE = "";

    private PathProcessorHandler handler;

    @Reference(bind = "bindCommonPaths", unbind = "unbindCommonPaths")
    private CommonPaths paths;

    protected void bindCommonPaths(CommonPaths paths) {
        this.paths = paths;
    }

    protected void unbindCommonPaths(CommonPaths paths) {
        this.paths = null;
    }

    @Activate
    public void activate() {
        File systemPluginRoot = paths.getSystemPluginRoot();
        File userPluginRoot = paths.getUserPluginRoot();
        File systemLibRoot = paths.getSystemLibRoot();

        JarLocations locations = new JarLocations();
        locations.getLocations().add(systemPluginRoot.toPath());
        locations.getLocations().add(userPluginRoot.toPath());
        // System lib root shall not be scanned recursively. See BundleManagerImpl.
        for (File f: systemLibRoot.listFiles()) {
            locations.getLocations().add(f.toPath());
        }

        handler = new PathProcessorHandler(locations);
    }

    public DependencyAnalyzerCommand() {}

    @Override
    public void run(CommandContext ctx) throws CommandException {

        if (ctx.getArguments().hasArgument(Args.LIST_ALL)) {
            ListAllAction.execute(handler, ctx);
        }

        if (ctx.getArguments().hasArgument(Args.EXPORTS)) {
            String library = findJarPath(handler, ctx.getArguments().getArgument(Args.EXPORTS), ctx);
            if (!NO_JAR_FILE_FOR_PACKAGE.equals(library)) {
                PrintOSGIHeaderAction.execute(library, ctx, BundleProperties.EXPORT);
            }
        }

        if (ctx.getArguments().hasArgument(Args.IMPORTS)) {
            String library = findJarPath(handler, ctx.getArguments().getArgument(Args.IMPORTS), ctx);
            if (!NO_JAR_FILE_FOR_PACKAGE.equals(library)) {
                PrintOSGIHeaderAction.execute(library, ctx, BundleProperties.IMPORT);
            }
        }

        if (ctx.getArguments().hasArgument(Args.OUTBOUND)) {
            String library = findJarPath(handler, ctx.getArguments().getArgument(Args.OUTBOUND), ctx);
            if (!NO_JAR_FILE_FOR_PACKAGE.equals(library)) {
                ListDependenciesAction.execute(handler, library, ctx);
            }
        }

        if (ctx.getArguments().hasArgument(Args.INBOUND)) {
            String library = findJarPath(handler, ctx.getArguments().getArgument(Args.INBOUND), ctx);
            if (!NO_JAR_FILE_FOR_PACKAGE.equals(library)) {
                ListDependenciesAction.execute(handler, library, ctx, true);
            }
        }

        if (ctx.getArguments().hasArgument(Args.PROVIDES)) {
            String target = ctx.getArguments().getArgument(Args.PROVIDES);
            SearchPackageAction.execute(handler, target, ctx);
        }
    }

    static String findJarPath(PathProcessorHandler handler, String target, CommandContext ctx) {
        Matcher m = JAR_PATTERN.matcher(String.valueOf(target));
        if (!m.matches()) {
            target = SearchPackageAction.execute(handler, target, ctx);
        }
        return target;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

}