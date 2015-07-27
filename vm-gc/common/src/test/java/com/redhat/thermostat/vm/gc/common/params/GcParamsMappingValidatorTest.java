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

package com.redhat.thermostat.vm.gc.common.params;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.fail;

public class GcParamsMappingValidatorTest {

    @Test
    public void validateEmptyGcParamsMapping() throws IOException {
        String config = "<?xml version=\"1.0\"?>\n";
        File testFile = createFile("testSystemId", config);
        GcParamsMappingValidator validator = new GcParamsMappingValidator();
        try {
            validator.validate("testSystemId", new FileInputStream(testFile));
            fail("should not come here");
        } catch (GcParamsMappingValidatorException e) {
            //pass
        } finally {
            testFile.delete();
        }
    }

    @Test
    public void canValidateGcParamsMappingXMLMultipleTimes() throws Exception {
        try {
            String config = "<v1:gc-params-mapping xmlns:v1=\"http://icedtea.classpath.org/thermostat/gc-params-mapping/v1.0\">\n" +
                    "  <!--1 or more repetitions:-->\n" +
                    "  <v1:collector>\n" +
                    "    <v1:collector-info>\n" +
                    "      <v1:version>string</v1:version>\n" +
                    "      <v1:common-name>string</v1:common-name>\n" +
                    "      <v1:collector-distinct-names>\n" +
                    "        <!--1 or more repetitions:-->\n" +
                    "        <v1:collector-name>string</v1:collector-name>\n" +
                    "      </v1:collector-distinct-names>\n" +
                    "      <v1:url>string</v1:url>\n" +
                    "    </v1:collector-info>\n" +
                    "    <v1:gc-params>\n" +
                    "      <!--Zero or more repetitions:-->\n" +
                    "      <v1:gc-param>\n" +
                    "        <v1:flag>string</v1:flag>\n" +
                    "        <v1:description>string</v1:description>\n" +
                    "        <!--Optional:-->\n" +
                    "        <v1:version>string</v1:version>\n" +
                    "      </v1:gc-param>\n" +
                    "    </v1:gc-params>\n" +
                    "  </v1:collector>\n" +
                    "</v1:gc-params-mapping>";
            File testFile = createFile("testSystemId", config);
            GcParamsMappingValidator validator = new GcParamsMappingValidator();
            validator.validate("testSystemId", new FileInputStream(testFile));

            //  Second validation on the same file
            validator.validate("testSystemId", new FileInputStream(testFile));
            testFile.delete();
        } catch (GcParamsMappingValidatorException e) {
            fail("should not reach here, gc-params-mapping.xml should be validated according to schema");
        }
    }

    @Test
    public void validationFailsOnInvalidGcParamsMappingFile() throws Exception {
        String config = "<v1:gc-params-mapping xmlns:v1=\"http://icedtea.classpath.org/thermostat/gc-params-mapping/v1.0\">\n" +
                "  <!--1 or more repetitions:-->\n" +
                "  <v1:collector>\n" +
                "    <v1:collector-info>\n" +
                "      <!-- <v1:version>string</v1:version> -->\n" +
                "      <v1:common-name>string</v1:common-name>\n" +
                "      <v1:collector-distinct-names>\n" +
                "        <!--1 or more repetitions:-->\n" +
                "        <v1:collector-name>string</v1:collector-name>\n" +
                "      </v1:collector-distinct-names>\n" +
                "      <v1:url>string</v1:url>\n" +
                "    </v1:collector-info>\n" +
                "    <v1:gc-params>\n" +
                "      <!--Zero or more repetitions:-->\n" +
                "      <v1:gc-param>\n" +
                "        <!-- <v1:flag>string</v1:flag> -->\n" +
                "        <v1:description>string</v1:description>\n" +
                "        <!--Optional:-->\n" +
                "        <v1:version>string</v1:version>\n" +
                "      </v1:gc-param>\n" +
                "    </v1:gc-params>\n" +
                "  </v1:collector>\n" +
                "</v1:gc-params-mapping>";

        File testFile = createFile("testSystemId", config);
        GcParamsMappingValidator validator = new GcParamsMappingValidator();
        try {
            validator.validate("testSystemId", new FileInputStream(testFile));
            fail("gc-params-mapping.xml should not validate according to schema");
        } catch (GcParamsMappingValidatorException e) {
            //pass
        } finally {
            testFile.delete();
        }
    }

    @Test
    public void canValidateCorrectFile() throws IOException {
        String config = "<v1:gc-params-mapping xmlns:v1=\"http://icedtea.classpath.org/thermostat/gc-params-mapping/v1.0\">\n" +
                "  <v1:collector>\n" +
                "    <v1:collector-info>\n" +
                "      <v1:version>1.0.0_0:1.8.0_45</v1:version>\n" +
                "      <v1:common-name>G1</v1:common-name>\n" +
                "      <v1:collector-distinct-names>\n" +
                "        <v1:collector-name>G1 garbage collection</v1:collector-name>\n" +
                "      </v1:collector-distinct-names>\n" +
                "      <v1:url>http://example.com</v1:url>\n" +
                "    </v1:collector-info>\n" +
                "    <v1:gc-params>\n" +
                "      <v1:gc-param>\n" +
                "        <v1:flag>-XXUseG1GC</v1:flag>\n" +
                "        <v1:description>Enable G1 collector</v1:description>\n" +
                "        <v1:version>1.0.0_0:1.8.0_45</v1:version>\n" +
                "      </v1:gc-param>\n" +
                "      <v1:gc-param>\n" +
                "        <v1:flag>-XXExtremePerformanceFlag</v1:flag>\n" +
                "        <v1:description>Turbo Super Ultra Charged Performance</v1:description>\n" +
                "        <v1:version>1.2.0_0:1.8.0_45</v1:version>\n" +
                "      </v1:gc-param>\n" +
                "    </v1:gc-params>\n" +
                "  </v1:collector>\n" +
                "</v1:gc-params-mapping>";

        File testFile = createFile("testSystemId", config);
        GcParamsMappingValidator validator = new GcParamsMappingValidator();
        try {
            validator.validate("testSystemId", new FileInputStream(testFile));
        } catch (GcParamsMappingValidatorException e) {
            fail("should not reach here, gc-params-mapping.xml should be validated according to schema");
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
