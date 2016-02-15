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

package com.redhat.thermostat.annotations.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * An annotation processor that runs and finds {@code @Service} and
 * {@code @ExtensionPoint} annotations. A list of classes using these
 * annotations are written to a
 * {@code META-INF/thermostat/plugin-docs.xml} file.
 */
public class AnnotationProcessor extends AbstractProcessor {

    private enum ExposedAs {
        META("meta"),
        EXTENSION_POINT("extension-point"),
        SERVICE("service"),
        ;

        private String elementName;

        private ExposedAs(String elementName) {
            this.elementName = elementName;
        }

        private String getElementName() {
            return elementName;
        }
    }

    private boolean firstRound = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        firstRound = true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!firstRound) {
            return false;
        }

        firstRound = false;

        processingEnv.getMessager().printMessage(Kind.NOTE, "Searching for Service and ExtensionPoint annotations");

        List<PluginPointInformation> points = findPluginPoints(annotations, roundEnv);

        processingEnv.getMessager().printMessage(Kind.NOTE, "found " + points.size() + " classes useful for plugins");

        Element[] sourceElements = new Element[points.size()];
        for (int i = 0; i < points.size(); i++) {
            sourceElements[i] = points.get(i).annotatedClass;
        }

        if (points.size() > 0) {
            try {
                FileObject filer = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF" + File.separator + "thermostat" + File.separator + "plugin-docs.xml",
                        sourceElements);
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(filer.openOutputStream(), "UTF-8"))) {
                    writeXml(writer, points);
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Error writing to docs file: " + e.getMessage());
            }
        }

        return false;
    }

    private List<PluginPointInformation> findPluginPoints(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<PluginPointInformation> pluginPointInfo = new ArrayList<>();

        for (TypeElement annotation : annotations) {
            ExposedAs exposedType = null;
            if (annotation.getSimpleName().toString().contains("Service")) {
                exposedType = ExposedAs.SERVICE;
            } else if (annotation.getSimpleName().toString().contains("ExtensionPoint")) {
                exposedType = ExposedAs.EXTENSION_POINT;
            } else if (annotation.getSimpleName().toString().contains("Documented")) {
                exposedType = ExposedAs.META;
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING, "Unrecognized annotation: " + annotation.getSimpleName());
                continue;
            }

            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                TypeElement te = (TypeElement) element;
                pluginPointInfo.add(new PluginPointInformation(te, exposedType, processingEnv.getElementUtils().getDocComment(te)));
            }
        }
        return pluginPointInfo;
    }

    private void writeXml(PrintWriter writer, List<PluginPointInformation> points) {
        JavadocToXmlConverter converter = new JavadocToXmlConverter();

        writer.println("<?xml version=\"1.0\"?>");

        writer.println("<!-- autogenerated by " + this.getClass().getName() + " -->");

        writer.println("  <plugin-docs>");
        for (PluginPointInformation info: points) {
            String tag = info.exposedAs.getElementName();

            writer.println("    <" + tag + ">");
            writer.println("      <name>" + info.annotatedClass.getQualifiedName() + "</name>");
            if (info.javadoc != null) {
                writer.println("      <doc>");
                writer.println(converter.convert(info.javadoc));
                writer.println("      </doc>");
            }
            writer.println("    </" + tag + ">");
        }

        writer.println("  </plugin-docs>");
    }

    private static class PluginPointInformation {
        private TypeElement annotatedClass;
        private ExposedAs exposedAs;
        private String javadoc;

        public PluginPointInformation(TypeElement annotatedClass, ExposedAs exposedAs, String javadoc) {
            this.annotatedClass = annotatedClass;
            this.exposedAs = exposedAs;
            this.javadoc = javadoc;
        }
    }
}

