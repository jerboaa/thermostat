/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.plugin.validator.internal;

import java.io.File;

import com.redhat.thermostat.plugin.validator.ValidationIssue;

/**
 * 
 * Exception thrown on XML validation errors of thermostat-plugin.xml files.
 *
 */
public abstract class AbstractValidationError implements ValidationIssue {

    private int lineNumber;
    private int columnNumber;
    private String message;
    private File xmlFile;
    
    public AbstractValidationError(int lineNumber, int columnNumber, String message, File xmlFile) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.message = message;
        this.xmlFile = xmlFile;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public File getXmlFile() {
        return xmlFile;
    }
    
    public String toString() {
        String LS = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder();
        
        builder.append("[").append(getName()).append("]").append(LS);
        builder.append("   Message: ").append(getMessage()).append(LS);
        builder.append("   File: ").append(getXmlFile().getPath()).append(LS);
        builder.append("   Line number: ").append(getLineNumber()).append(LS);
        builder.append("   Column number: ").append(getColumnNumber()).append(LS);
        
        return builder.toString();
        
    }

    public abstract String getName();

}

