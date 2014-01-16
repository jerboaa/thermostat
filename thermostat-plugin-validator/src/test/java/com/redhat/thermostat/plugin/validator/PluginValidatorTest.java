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

package com.redhat.thermostat.plugin.validator;


import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;


public class PluginValidatorTest {
    
    @Test
    public void validateEmptyConfiguration() throws IOException {
        String config = "<?xml version=\"1.0\"?>\n";
        File testFile = createFile("testSystemId", config);
        PluginValidator validator = new PluginValidator();
        try {
            validator.validate(testFile);
            fail("should not come here");
        } catch (PluginConfigurationValidatorException e) {
            //pass
        } finally {
            testFile.delete();
        }
    }
    
    @Test
    public void canValidatePluginXMLMultipleTimes() throws Exception {
        
        try {
            String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermostat-plugin.xsd\">\n" +
                    "  <extensions>\n" +
                    "    <extension>\n" +
                    "      <name>test</name>\n" +
                    "      <bundles>\n" +
                    "        <bundle><symbolic-name>foo</symbolic-name><version>1</version></bundle>\n" +
                    "        <bundle><symbolic-name>bar</symbolic-name><version>2</version></bundle>\n" +
                    "        <bundle><symbolic-name>baz</symbolic-name><version>3</version></bundle>\n" +
                    "      </bundles>\n" +
                    "    </extension>\n" +
                    "  </extensions>\n" +
                    "</plugin>";
            File testFile = createFile("testSystemId", config);
            PluginValidator validator = new PluginValidator();
            validator.validate(testFile);
            
            //  Second validation on the same file
            validator.validate(testFile);
            testFile.delete();
        } catch (PluginConfigurationValidatorException e) {
           fail("should not reach here, plugin.xml should be validated according to schema");
        }
    }
    
    @Test
    public void validationFailsOnInvalidPluginFile() throws Exception {
        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermostat-plugin.xsd\">\n" +
                "  <extensions>\n" +      
                "    <something>\n" +    // Error line
                "    <extension>\n" +
                "      <name>test</name>\n" +
                "      <bundles>\n" +
                "        <bundle>foo</bundle>\n" +   // Error line
                "        <bundle><symbolic-name>bar</symbolic-name><version>2</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name></bundle>\n" +   // Error line
                "      </bundles>\n" +
                "      <dependencies>\n" +   // Error line
                "        <dependency>thermostat-foo</dependency>\n" +   // Error line
                "      </dependencies>\n" +   // Error line
                "    </extension>\n" +
                "  </extensions>\n" +
                "</plugin>";

        File testFile = createFile("testSystemId", config);
        PluginValidator validator = new PluginValidator();
        try {
            validator.validate(testFile);
            fail("plugin.xml should not validate according to schema");
        } catch (PluginConfigurationValidatorException e) {
            //pass
        } finally {
            testFile.delete();
        }
    }
    
    @Test
    public void canValidateCorrectFile() throws IOException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermostat-plugin.xsd\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <group>\n" +
                "          <required>true</required>\n" +
                "          <option>\n" +
                "            <long>exclusive-a</long>\n" +
                "            <short>a</short>\n" +
                "            <argument>false</argument>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option a</description>\n" +
                "          </option>\n" +
                "          <option>\n" +
                "            <long>exclusive-b</long>\n" +
                "            <short>b</short>\n" +
                "            <argument>false</argument>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option b</description>\n" +
                "          </option>\n" +
                "        </group>\n" +
                "        <option>\n" +
                "          <long>long</long>\n" +
                "          <short>l</short>\n" +
                "          <argument>true</argument>\n" +
                "          <required>true</required>\n" +
                "          <description>some required and long option</description>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "      <environments>\n" +
                "        <environment>cli</environment>\n" +
                "      </environments>\n" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>foo</symbolic-name><version>1</version></bundle>\n" +
                "        <bundle><symbolic-name>bar</symbolic-name><version>2</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name><version>3</version></bundle>\n" +
                "      </bundles>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        File testFile = createFile("testSystemId", config);
        PluginValidator validator = new PluginValidator();
        try {
            validator.validate(testFile);
        } catch (PluginConfigurationValidatorException e) {
            fail("should not reach here, plugin.xml should be validated according to schema");
        } finally {
            testFile.delete();
        }
        
    }
    
    private File createFile(String fileName, String contents) throws IOException {
        FileWriter fstream = new FileWriter(fileName);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(contents);
        out.close();
        return new File(fileName);
    }

}

