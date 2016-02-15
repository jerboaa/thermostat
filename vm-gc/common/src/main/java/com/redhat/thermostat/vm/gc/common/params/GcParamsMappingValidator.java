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

package com.redhat.thermostat.vm.gc.common.params;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class GcParamsMappingValidator {

    static final String XSD_RESOURCE_URL = "/com/redhat/thermostat/vm/gc/common/gc-params-mapping.xsd";

    public void validate(String path, InputStream gcParamsMappingXmlStream) throws GcParamsMappingValidatorException, FileNotFoundException {
        URL schemaUrl = GcParamsMappingValidator.class.getResource(XSD_RESOURCE_URL);
        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        GcParamsMappingValidatorErrorHandler handler = new GcParamsMappingValidatorErrorHandler(path);

        try {
            Schema schema = schemaFactory.newSchema(schemaUrl);
            Validator validator = schema.newValidator();
            validator.setErrorHandler(handler);
            validator.validate(new StreamSource(gcParamsMappingXmlStream));
        } catch (SAXException exception) {
            throw new GcParamsMappingValidatorException(
                    "gc-params-mapping.xml not well-formed XML", path,
                    handler.getErrors(), handler.getWarnings(),
                    handler.getFatalErrors(), exception);
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException ioe) {
            throw new GcParamsMappingValidatorException(
                    ioe.getLocalizedMessage(), path, handler.getErrors(),
                    handler.getWarnings(), handler.getFatalErrors(), ioe);
        }

        // We've registered an exception handler, so validation issues do not throw
        // exceptions unless the xml file itself is not valid. Let's ask the handler
        // if there were issues so we can throw an exception accordingly.
        if (handler.hasValidationIssues()) {
            throw new GcParamsMappingValidatorException(
                    "Failed to validate gc-params-mapping.xml", path,
                    handler.getErrors(), handler.getWarnings(),
                    handler.getFatalErrors());
        }
    }

}