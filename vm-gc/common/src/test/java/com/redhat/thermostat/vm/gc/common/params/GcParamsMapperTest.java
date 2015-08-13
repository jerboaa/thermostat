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

import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GcParamsMapperTest {

    public static final JavaVersionRange JAVA_VERSION = new JavaVersionRange(new JavaVersionRange.VersionPoints(1, 8, 0, 45));
    private final GcParamsMapper paramsMapper = GcParamsMapper.getInstance();

    @Test
    public void testXsdValidatesXml() {
        Exception ex = null;
        String exceptionMessage = null;
        try {
            InputStream stream = paramsMapper.getXmlStream();
            GcParamsMappingValidator validator = new GcParamsMappingValidator();
            validator.validate(GcParamsMapper.XML_RESOURCE_URL, stream);
        } catch (GcParamsMappingValidatorException e) {
            ValidationErrorsFormatter formatter = new ValidationErrorsFormatter();
            exceptionMessage = formatter.format(e.getAllErrors());
            ex = e;
        } catch (FileNotFoundException | URISyntaxException e) {
            exceptionMessage = e.getMessage();
            ex = e;
        }
        assertTrue(exceptionMessage, ex == null);
    }

    @Test
    public void testUnknownHasNoParams() {
        List<GcParam> params = paramsMapper.getParams(GcCommonNameMapper.CollectorCommonName.UNKNOWN_COLLECTOR, JAVA_VERSION);
        assertEquals(0, params.size());
    }

    @Test
    public void testNoEmptyFlags() {
        List<Collector> collectors = paramsMapper.getCollectors();
        for (Collector collector : collectors) {
            for (GcParam param : collector.getGcParams()) {
                String flag = param.getFlag();
                assertFalse(collector.getCollectorInfo().getCommonName(), flag == null || flag.isEmpty());
            }
        }
    }

    @Test
    public void testTunableVersionsContainedByCollectorVersions() {
        List<Collector> collectors = paramsMapper.getCollectors();
        for (Collector collector : collectors) {
            JavaVersionRange collectorVersion = collector.getCollectorInfo().getJavaVersionRange();
            for (GcParam param : collector.getGcParams()) {
                JavaVersionRange paramVersion = param.getJavaVersionRange();
                assertTrue(param.getFlag() + " lower", collectorVersion.contains(paramVersion.getLowerBound()));
                assertTrue(param.getFlag() + " upper", collectorVersion.contains(paramVersion.getUpperBound()));
            }
        }
    }

}
