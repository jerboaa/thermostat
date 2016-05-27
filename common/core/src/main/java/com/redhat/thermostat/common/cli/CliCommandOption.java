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

/**
 * Used for describing command line flags or options.
 *
 * Example: in "foo-command --help", a CliCommandOption could be used to describe "--help".
 */
public class CliCommandOption {

    /**
     * Special constant for use with {@link CompleterService}, which indicates a "meta-option" which is really
     * the first non-option argument. Use this constant if you are implementing a command which takes a
     * non-option argument and for which you intend to provide tab completions.
     */
    public static final CliCommandOption POSITIONAL_ARG_COMPLETION = new CliCommandOption("__NO_ARG__", "__NO_ARG__");

    private final String opt;
    private final String longOpt;
    private final boolean hasArg;
    private final String description;
    private final boolean required;

    private CliCommandOption(String opt, String description) {
        this(opt, null, false, description, false);
    }

    /**
     * @param opt the short-option, ex. in the case of "-h", this should be "h"
     * @param longOpt the long-option, ex. in the case of "--help", this should be "help"
     * @param hasArg defines whether or not this option expects an argument
     * @param description a description of what the option does
     * @param required defines whether or not this option is optional or must be included in a command invocation
     */
    public CliCommandOption(String opt, String longOpt, boolean hasArg, String description, boolean required) {
        this.opt = opt;
        this.longOpt = longOpt;
        this.hasArg = hasArg;
        this.description = description;
        this.required = required;
    }

    public String getOpt() {
        return opt;
    }

    public String getLongOpt() {
        return longOpt;
    }

    public boolean hasArg() {
        return hasArg;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

}
