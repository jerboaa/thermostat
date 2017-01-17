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

package com.redhat.thermostat.platform.internal.annotations.processor;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.annotations.ApplicationDescriptor;
import com.redhat.thermostat.platform.internal.application.ApplicationInfo;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 */
@SupportedAnnotationTypes("com.redhat.thermostat.platform.annotations.ApplicationDescriptor")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class PlatformConfigurationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv)
    {
        if(roundEnv.processingOver()) {
            return false;
        }

        ApplicationInfo info = new ApplicationInfo();
        info.applications = new ArrayList<>();

        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();
        String applicationProvider =
                ApplicationProvider.class.getCanonicalName();
        
        TypeMirror providerInterface =
                elements.getTypeElement(applicationProvider).asType();
        
        Messager messager = processingEnv.getMessager();
        for (Element element :
             roundEnv.getElementsAnnotatedWith(ApplicationDescriptor.class))
        {
            messager.printMessage(Diagnostic.Kind.NOTE,
                                  "for element: " + element);
            
            boolean isSame = types.isSubtype(element.asType(), providerInterface);

            if (!isSame) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                                      "Cannot process element " + element +
                                      " - must be of type: " +
                                      ApplicationProvider.class);
                continue;
            }
            
            ApplicationDescriptor descriptor =
                    element.getAnnotation(ApplicationDescriptor.class);
            ApplicationInfo.Application data =
                    new ApplicationInfo.Application();

            data.name = descriptor.name();
            data.provider = element.asType().toString() + ".class";

            messager.printMessage(Diagnostic.Kind.NOTE,
                                  "application provider class: " +
                                  data.provider);
            messager.printMessage(Diagnostic.Kind.NOTE,
                                  "application name: " + data.name);
            
            info.applications.add(data);
        }

        if (!info.applications.isEmpty()) {
            writeConfigurations(messager, info);
        }

        return false;
    }

    private void writeConfigurations(Messager messager, ApplicationInfo info) {
        Map<String, String> options = processingEnv.getOptions();
        String jsonFileName = options.get("name");
        if (jsonFileName == null) {
            jsonFileName = info.applications.get(0).name;
        }

        Gson gson = new GsonBuilder().
                setPrettyPrinting().
                setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).
                create();
        String json = gson.toJson(info) + "\n";

        messager.printMessage(Diagnostic.Kind.NOTE,
                              "writing json configuration...");

        try {
            Filer filer = processingEnv.getFiler();
            FileObject output =
                    filer.createResource(StandardLocation.SOURCE_OUTPUT,
                                         "",
                                         jsonFileName  + ".json");

            Writer writer = output.openWriter();
            writer.append(json);
            writer.flush();

            messager.printMessage(Diagnostic.Kind.NOTE, "done!");

        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                                  "cannot write json configuration file");

        }
    }
}
