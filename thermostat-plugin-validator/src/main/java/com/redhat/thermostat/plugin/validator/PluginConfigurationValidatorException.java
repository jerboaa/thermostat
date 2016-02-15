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

package com.redhat.thermostat.plugin.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Thrown if there were schema validation issues or other problems in a
 * thermostat-plugin.xml
 * 
 */
public class PluginConfigurationValidatorException extends Exception {

    private static final long serialVersionUID = 1L;
    private final File xmlFile;
    private final List<ValidationIssue> warnings;
    private final List<ValidationIssue> errors;
    private final List<ValidationIssue> fatals;

    /**
     * Constructor.
     * 
     * Calls
     * {@link #PluginConfigurationValidatorException(String, File, List, List, List, Throwable)}
     * with a null cause.
     */
    public PluginConfigurationValidatorException(String message, File xmlFile,
            List<ValidationIssue> errors, List<ValidationIssue> warnings,
            List<ValidationIssue> fatals) {
        this(message, xmlFile, errors, warnings, fatals, null);
    }

    /**
     * Constructor.
     * 
     * @param message
     *            A descriptive message.
     * @param xmlFile
     *            The thermostat-plugin.xml file which caused this exception to
     *            be thrown.
     * @param errors
     *            The list of schema validation errors.
     * @param warnings
     *            The list of (validation) warnings.
     * @param fatals
     *            The list of fatal (validation) errors.
     * @param cause
     *            The underlying exception. May be null.
     */
    public PluginConfigurationValidatorException(String message, File xmlFile,
            List<ValidationIssue> errors, List<ValidationIssue> warnings,
            List<ValidationIssue> fatals, Throwable cause) {
        super(message);
        this.xmlFile = xmlFile;
        this.warnings = warnings;
        this.errors = errors;
        this.fatals = fatals;
    }

    /**
     * 
     * @return The list of all validation issues.
     */
    public List<ValidationIssue> getAllErrors() {
        List<ValidationIssue> errorsList = new ArrayList<>();
        errorsList.addAll(warnings);
        errorsList.addAll(errors);
        errorsList.addAll(fatals);
        return errorsList;
    }

    /**
     * 
     * @return The thermostat-plugin.xml file which failed validation.
     */
    public File getXmlFile() {
        return xmlFile;
    }

    /**
     * Conditions that are not errors or fatal errors as defined by the XML
     * recommendation.
     * 
     * @return The list of (validation) warnings.
     */
    public List<ValidationIssue> getWarnings() {
        return warnings;
    }

    /**
     * Each validation issue corresponds to the definition of "error" in section
     * 1.2 of the W3C XML 1.0 Recommendation.
     * 
     * @return The list of violations of schema validity constraints.
     */
    public List<ValidationIssue> getErrors() {
        return errors;
    }

    /**
     * Each validation issue corresponds to the definition of "fatal error" in
     * section 1.2 of the W3C XML 1.0 Recommendation.
     * 
     * @return The list of fatal (i.e. non-recoverable) errors. Fatal issues may
     *         occur if the thermostat-plugin.xml violates well-formedness
     *         constraints.
     */
    public List<ValidationIssue> getFatals() {
        return fatals;
    }

}

