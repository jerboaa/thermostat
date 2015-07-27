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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GcParamsParser {

    private static final String ROOT_NODE_NAME = "gc-params-mapping";
    private static final String COLLECTOR_NODE_NAME = "collector";
    private static final String COLLECTOR_INFO_NODE_NAME = "collector-info";
    private static final String GC_PARAMS_NODE_NAME = "gc-params";
    private static final String VERSION_NODE_NAME = "version";
    private static final String COMMON_NAME_NODE_NAME = "common-name";
    private static final String COLLECTOR_DISTINCT_NAMES_NODE_NAME = "collector-distinct-names";
    private static final String COLLECTOR_DISTINCT_NAME_NODE_NAME = "collector-name";
    private static final String REFERENCE_URL_NODE_NAME = "url";
    private static final String GC_PARAM_NODE_NAME = "gc-param";
    private static final String FLAG_NODE_NAME = "flag";
    private static final String DESCRIPTION_NODE_NAME = "description";

    private final InputStream xmlStream;

    public GcParamsParser(InputStream xmlStream) {
        this.xmlStream = xmlStream;
    }

    public List<Collector> parse() throws GcParamsParseException, IOException {
        return parse(xmlStream);
    }

    static List<Collector> parse(InputStream inputStream) throws GcParamsParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new GcParamsParserErrorHandler());
            Document xmlDoc = builder.parse(inputStream);
            Node rootNode = xmlDoc.getFirstChild();
            if (rootNode == null) {
                throw new GcParamsParseException("Invalid document, could not identify root node");
            }
            return parseRootElement(rootNode);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new GcParamsParseException(e);
        }
    }

    static List<Collector> parseRootElement(Node rootNode) {
        List<Collector> collectors = new ArrayList<>();
        if (rootNode.getNodeName().equals(ROOT_NODE_NAME)) {
            NodeList nodes = rootNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(COLLECTOR_NODE_NAME)) {
                    collectors.add(parseCollector(node));
                }
            }
        }
        return collectors;
    }

    static Collector parseCollector(Node collectorNode) {
        CollectorInfo collectorInfo = null;
        Set<GcParam> gcParams = Collections.emptySet();
        if (collectorNode.getNodeName().equals(COLLECTOR_NODE_NAME)) {
            NodeList nodes = collectorNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(COLLECTOR_INFO_NODE_NAME)) {
                    collectorInfo = parseCollectorInfo(node);
                } else if (node.getNodeName().equals(GC_PARAMS_NODE_NAME)) {
                    gcParams = parseGcParams(node, collectorInfo.getJavaVersion());
                }
            }
        }
        return new Collector(collectorInfo, gcParams);
    }

    static CollectorInfo parseCollectorInfo(Node collectorInfoNode) {
        JavaVersion javaVersion = null;
        String commonName = "";
        Set<String> collectorDistinctNames = Collections.emptySet();
        String referenceUrl = "";
        if (collectorInfoNode.getNodeName().equals(COLLECTOR_INFO_NODE_NAME)) {
            NodeList nodes = collectorInfoNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(VERSION_NODE_NAME)) {
                    javaVersion = parseVersion(node);
                } else if (node.getNodeName().equals(COMMON_NAME_NODE_NAME)) {
                    commonName = parseCommonName(node);
                } else if (node.getNodeName().equals(COLLECTOR_DISTINCT_NAMES_NODE_NAME)) {
                    collectorDistinctNames = parseDistinctNames(node);
                } else if (node.getNodeName().equals(REFERENCE_URL_NODE_NAME)) {
                    referenceUrl = parseReferenceUrl(node);
                }
            }
        }
        return new CollectorInfo(javaVersion, commonName, collectorDistinctNames, referenceUrl);
    }

    static JavaVersion parseVersion(Node versionNode) {
        JavaVersion javaVersion = null;
        if (versionNode.getNodeName().equals(VERSION_NODE_NAME)) {
            try {
                javaVersion = JavaVersion.fromString(versionNode.getTextContent());
            } catch (JavaVersion.InvalidJavaVersionFormatException e) {
                throw new GcParamsParseException(e);
            }
        }
        return javaVersion;
    }

    static String parseCommonName(Node commonNameNode) {
        String commonName = "";
        if (commonNameNode.getNodeName().equals(COMMON_NAME_NODE_NAME)) {
            commonName = commonNameNode.getTextContent();
        }
        return commonName;
    }

    static Set<String> parseDistinctNames(Node distinctNamesNode) {
        Set<String> distinctNames = new HashSet<>();
        if (distinctNamesNode.getNodeName().equals(COLLECTOR_DISTINCT_NAMES_NODE_NAME)) {
            NodeList nodes = distinctNamesNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(COLLECTOR_DISTINCT_NAME_NODE_NAME)) {
                    distinctNames.add(parseDistinctName(node));
                }
            }
        }
        return distinctNames;
    }

    static String parseDistinctName(Node distinctNameNode) {
        String distinctName = "";
        if (distinctNameNode.getNodeName().equals(COLLECTOR_DISTINCT_NAME_NODE_NAME)) {
            distinctName = distinctNameNode.getTextContent();
        }
        return distinctName;
    }

    static String parseReferenceUrl(Node referenceUrlNode) {
        String referenceUrl = "";
        if (referenceUrlNode.getNodeName().equals(REFERENCE_URL_NODE_NAME)) {
            referenceUrl = referenceUrlNode.getTextContent();
        }
        return referenceUrl;
    }

    static Set<GcParam> parseGcParams(Node gcParamsNode, JavaVersion inheritedJavaVersion) {
        Set<GcParam> gcParams = new HashSet<>();
        if (gcParamsNode.getNodeName().equals(GC_PARAMS_NODE_NAME)) {
            NodeList nodes = gcParamsNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(GC_PARAM_NODE_NAME)) {
                    gcParams.add(parseGcParam(node, inheritedJavaVersion));
                }
            }
        }
        return gcParams;
    }

    static GcParam parseGcParam(Node gcParamNode, JavaVersion inheritedJavaVersion) {
        String flag = "";
        String description = "";
        JavaVersion javaVersion = inheritedJavaVersion;
        if (gcParamNode.getNodeName().equals(GC_PARAM_NODE_NAME)) {
            NodeList nodes = gcParamNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(FLAG_NODE_NAME)) {
                    flag = parseFlag(node);
                } else if (node.getNodeName().equals(DESCRIPTION_NODE_NAME)) {
                    description = parseDescription(node);
                } else if (node.getNodeName().equals(VERSION_NODE_NAME)) {
                    javaVersion = parseVersion(node);
                }
            }
        }
        return new GcParam(flag, description, javaVersion);
    }

    static String parseFlag(Node flagNode) {
        String flag = "";
        if (flagNode.getNodeName().equals(FLAG_NODE_NAME)) {
            flag = flagNode.getTextContent();
        }
        return flag;
    }

    static String parseDescription(Node descriptionNode) {
        String description = "";
        if (descriptionNode.getNodeName().equals(DESCRIPTION_NODE_NAME)) {
            description = descriptionNode.getTextContent();
        }
        return description;
    }

    private static class GcParamsParserErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException e) throws SAXException {
            // no-op
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }

    public static class GcParamsParseException extends RuntimeException {
        public GcParamsParseException(String message) {
            super(message);
        }

        public GcParamsParseException(Throwable cause) {
            super(cause);
        }
    }
}
