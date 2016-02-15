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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class MergePluginDocs {

    private static final String CORE = "plugin-docs";
    private static final String DOCS_FILE_NAME = CORE + ".xml";
    private static final String DOCS_ELEMENT = CORE;

    private static final List<Path> corePaths = new LinkedList<>();

    public static void main(String[] args) throws IOException, XMLStreamException {
        corePaths.add(Paths.get("agent").toAbsolutePath().normalize());
        corePaths.add(Paths.get("launcher").toAbsolutePath().normalize());
        corePaths.add(Paths.get("client").toAbsolutePath().normalize());
        corePaths.add(Paths.get("common").toAbsolutePath().normalize());
        corePaths.add(Paths.get("keyring").toAbsolutePath().normalize());
        corePaths.add(Paths.get("storage", "core").toAbsolutePath().normalize());
        corePaths.add(Paths.get("config").toAbsolutePath().normalize());

        String startPath = ".";
        if (args.length > 0) {
            startPath = args[0];
        }

        List<Path> paths = findAllPluginDocs(startPath);
        String mergedXml = mergePluginDocs(paths);
        System.out.println(mergedXml);
    }

    private static List<Path> findAllPluginDocs(String startPath) throws IOException {
        final List<Path> paths = new LinkedList<>();

        Files.walkFileTree(Paths.get(startPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!shouldEnter(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals(DOCS_FILE_NAME)) {
                    paths.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    /** Only enter a directory if it is (or could lead to) a directory we want to check */
    private static boolean shouldEnter(Path toCheck) {
        toCheck = toCheck.toAbsolutePath().normalize();
        for (Path path : corePaths) {
            if (toCheck.startsWith(path) || path.startsWith(toCheck)) {
                return true;
            }
        }
        return false;
    }

    private static String mergePluginDocs(List<Path> paths) throws IOException, XMLStreamException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLEventFactory eventFactory = XMLEventFactory.newFactory();
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        XMLEventWriter writer = outputFactory.createXMLEventWriter(outputStream);
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();

        QName docsElement = new QName("", DOCS_ELEMENT, "");

        writer.add(eventFactory.createStartDocument());
        writer.add(eventFactory.createSpace("\n"));
        writer.add(eventFactory.createStartElement(docsElement, null, null));

        for (Path path : paths) {
            XMLEventReader reader = inputFactory.createXMLEventReader(Files.newInputStream(path));
            XMLEvent event;
            while (reader.hasNext()) {
                event = reader.nextEvent();
                if (event.getEventType() != XMLEvent.START_DOCUMENT && event.getEventType() != XMLEvent.END_DOCUMENT) {
                    if (event.isStartElement() && event.asStartElement().getName().equals(docsElement)) {
                        // skip
                    } else if (event.isEndElement() && event.asEndElement().getName().equals(docsElement)) {
                        // skip
                    } else if (event.getEventType() == XMLEvent.COMMENT) {
                        // skip
                    } else {
                        writer.add(event);
                    }
                }
            }
        }

        writer.add(eventFactory.createEndElement(docsElement, null));
        writer.add(eventFactory.createEndDocument());
        writer.close();

        return outputStream.toString("UTF-8");
    }

}

