/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class ListExportedClasses {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("\tjava ListExportedClass path/to/jar1 [path/to/jar2 [path/to/jar3 [...]]]");
            System.out.println();
            System.out.println("Prints a list of OSGi-exported packages and public classes in those packages");
        }

        List<String> jarNames = Arrays.asList(args);
        for (String jarName : jarNames) {
            System.out.println(jarName);
            try {
                JarFile jarFile = new JarFile(jarName);
                for (String exportedPackage : findOSGiExportedPackages(jarFile)) {
                    System.out.println("\t" + exportedPackage);
                    for (String exportedClass : findOSGiExportedClassesInPackage(jarFile, exportedPackage)) {
                        System.out.println("\t\t" + exportedClass);
                    }
                }
            } catch (IOException ioe) {
                System.err.println("error reading " + jarName);
            }
        }
    }

    private static List<String> findOSGiExportedClassesInPackage(JarFile jarFile, String packageName) throws IOException {
        List<String> exportedClasses = new ArrayList<>();

        List<String> allClasses = findClassNames(jarFile);

        for (String className : allClasses) {
            if (!packageName.equals(getPackageName(className))) {
                continue;
            }

            if (!isClassPublic(jarFile, className)) {
                continue;
            }

            exportedClasses.add(className);
        }
        Collections.sort(exportedClasses);
        return exportedClasses;
    }

    private static List<String> findClassNames(JarFile file) {
        List<String> result = new ArrayList<>();
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (!entryName.endsWith(".class")) {
                continue;
            }

            String className = entryName.substring(0, entryName.length() - ".class".length());
            className = className.replace('/', '.');
            result.add(className);
        }
        return result;
    }

    private static boolean isClassPublic(JarFile jarFile, String className) throws IOException {
        /*
         * we can not use reflection without loading the class itself (and its
         * dependencies). so lets just parse the class file itself.
         */

        ZipEntry entry = jarFile.getEntry(className.replace('.', '/').concat(".class"));
        InputStream classInputStream = jarFile.getInputStream(entry);
        DataInputStream in = new DataInputStream(classInputStream);

        int magic = in.readInt();
        if (!(magic == 0xCAFEBABE)) {
            throw new InternalError("Tried parsing a maformed class file");
        }
        /* int minorVersion = */in.readUnsignedShort();
        /* int majorVersion = */in.readUnsignedShort();
        int constPoolCount = in.readUnsignedShort();

        for (int i = 1; i < constPoolCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
            case 7:
            case 8:
                in.readUnsignedShort();
                break;
            case 3:
            case 4:
            case 9:
            case 10:
            case 11:
            case 12:
                in.readInt();
                break;
            case 5:
            case 6:
                in.readLong();
                i++; // these take up two entries in the pool
                break;
            case 1:
                int length = in.readUnsignedShort();
                for (int j = 0; j < length; j++) {
                    in.readUnsignedByte();
                }
                break;
            }
        }

        int accessFlags = in.readUnsignedShort();

        return (accessFlags & 0x0001) == 0x0001;
    }

    private static String getPackageName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fullyQualifiedClassName.substring(0, lastDot);
    }

    private static List<String> findOSGiExportedPackages(JarFile jarFile) throws IOException {
        Manifest manifest = jarFile.getManifest();
        String exportedPackagesString = manifest.getMainAttributes().getValue("Export-Package");
        if (exportedPackagesString == null) {
            return new ArrayList<>();
        }
        List<String> exportedPackages = Arrays.asList(exportedPackagesString.split(","));
        for (int i = 0; i < exportedPackages.size(); i++) {
            String exportedPackage = exportedPackages.get(i);
            // exports can have a ";uses.." part after the package name. remove it.
            int indexOfUses = exportedPackage.indexOf(";");
            if (indexOfUses != -1) {
                exportedPackage = exportedPackage.substring(0, indexOfUses);
            }
            exportedPackages.set(i, exportedPackage);
        }
        Collections.sort(exportedPackages);
        return exportedPackages;
    }
}
