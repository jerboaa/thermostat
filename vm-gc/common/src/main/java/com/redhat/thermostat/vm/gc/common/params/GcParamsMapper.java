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

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import com.redhat.thermostat.vm.gc.common.internal.LocaleResources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public enum GcParamsMapper {

    INSTANCE;

    static final String XML_RESOURCE_URL = "/com/redhat/thermostat/vm/gc/common/gc-params-mapping.xml";

    private final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private final Logger logger = LoggingUtils.getLogger(GcParamsMapper.class);
    private final Map<GcCommonNameMapper.CollectorCommonName, Collector> paramsMap = new HashMap<>();
    private final Set<GcParam> commonParams = new HashSet<>();

    GcParamsMapper() {
        validateGcParamsMapping();
        parseGcParamsMapping();
    }

    void validateGcParamsMapping() {
        InputStream xmlStream;
        try {
            xmlStream = getXmlStream();
            GcParamsMappingValidator validator = new GcParamsMappingValidator();
            validator.validate(XML_RESOURCE_URL, xmlStream);
        } catch (URISyntaxException | FileNotFoundException e) {
            logger.warning(translator.localize(
                    LocaleResources.VALIDATION_FAILED,
                    XML_RESOURCE_URL,
                    e.getLocalizedMessage())
                    .getContents());
        } catch (GcParamsMappingValidatorException e) {
            ValidationErrorsFormatter formatter = new ValidationErrorsFormatter();
            logger.warning(translator.localize(
                    LocaleResources.VALIDATION_FAILED,
                    XML_RESOURCE_URL,
                    formatter.format(e.getAllErrors()))
                    .getContents());
        }
    }

    InputStream getXmlStream() throws URISyntaxException {
        return this.getClass().getResourceAsStream(XML_RESOURCE_URL);
    }

    private void parseGcParamsMapping() {
        try {
            GcParamsParser parser = new GcParamsParser(getXmlStream());
            GcParamsParser.ParseResult parseResult = parser.parse();
            for (Collector collector : parseResult.getCollectors()) {
                GcCommonNameMapper commonNameMapper = new GcCommonNameMapper();
                paramsMap.put(commonNameMapper.mapToCommonName(collector.getCollectorInfo().getCollectorDistinctNames()), collector);
            }
            commonParams.addAll(parseResult.getGcCommonParams());
        } catch (URISyntaxException | IOException | GcParamsParser.GcParamsParseException e) {
            logger.warning("Failed to parse " + XML_RESOURCE_URL + " : " + e.getLocalizedMessage());
        }
    }

    List<Collector> getCollectors() {
        return new ArrayList<>(paramsMap.values());
    }

    Set<GcParam> getCommonParams() {
        return new HashSet<>(commonParams);
    }

    public List<GcParam> getParams(GcCommonNameMapper.CollectorCommonName collectorCommonName, JavaVersionRange javaVersionRange) {
        Objects.requireNonNull(collectorCommonName);
        Objects.requireNonNull(javaVersionRange);
        if (!paramsMap.containsKey(collectorCommonName)) {
            return Collections.emptyList();
        }
        Collector collector = paramsMap.get(collectorCommonName);
        if (collector == null || !collector.getCollectorInfo().getJavaVersionRange().contains(javaVersionRange)) {
            return Collections.emptyList();
        }
        List<GcParam> params = new ArrayList<>();
        for (GcParam param : collector.getGcParams()) {
            if (param.getJavaVersionRange().contains(javaVersionRange)) {
                params.add(param);
            }
        }
        params.addAll(commonParams);
        return params;
    }

    public static GcParamsMapper getInstance() {
        return INSTANCE;
    }

}
