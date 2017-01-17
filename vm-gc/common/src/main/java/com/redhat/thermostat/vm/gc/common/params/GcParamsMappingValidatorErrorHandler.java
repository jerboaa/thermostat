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

package com.redhat.thermostat.vm.gc.common.params;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class GcParamsMappingValidatorErrorHandler implements ErrorHandler {

    private final String gcParamsMappingXmlFilePath;
    private final List<ValidationIssue> warnings;
    private final List<ValidationIssue> errors;
    private final List<ValidationIssue> fatalErrors;

    public GcParamsMappingValidatorErrorHandler(String gcParamsMappingXmlFilePath) {
        this.gcParamsMappingXmlFilePath = gcParamsMappingXmlFilePath;
        warnings = new ArrayList<>();
        errors = new ArrayList<>();
        fatalErrors = new ArrayList<>();
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        Warning newWarning = new Warning(exception.getLineNumber(),
                exception.getColumnNumber(),
                exception.getLocalizedMessage(),
                gcParamsMappingXmlFilePath);
        warnings.add(newWarning);
    }

    @Override
    public void error(SAXParseException exception) throws SAXParseException {
        Error newError = new Error(exception.getLineNumber(),
                exception.getColumnNumber(),
                exception.getLocalizedMessage(),
                gcParamsMappingXmlFilePath);
        errors.add(newError);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXParseException {
        //  Fatal errors will be reported just when no validation warnings and errors happened. 
        //  In this way we avoid wrong messages of bad form for files that have wrong tags not closed properly
        if (errors.size() == 0 && warnings.size() == 0) {
            FatalError newFatalError = new FatalError(exception.getLineNumber(),
                    exception.getColumnNumber(),
                    exception.getLocalizedMessage(),
                    gcParamsMappingXmlFilePath);
            fatalErrors.add(newFatalError);
        }
    }

    public List<ValidationIssue> getWarnings() {
        return warnings;
    }

    public List<ValidationIssue> getErrors() {
        return errors;
    }

    public List<ValidationIssue> getFatalErrors() {
        return fatalErrors;
    }

    public boolean hasValidationIssues() {
        return errors.size() > 0 ||
                warnings.size() > 0 ||
                fatalErrors.size() > 0;
    }

}

