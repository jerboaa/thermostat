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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.locale.Translate;

public class BuiltInCommandInfoTest {

    private Path tempThermostatHome, someJarName1, someJarName2, missingJarName;
    private File tempLibs;

    @Before
    public void setUp() throws IOException {
        tempThermostatHome = Files.createTempDirectory("test");
        tempThermostatHome.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempThermostatHome.toString());

        tempLibs = new File(tempThermostatHome.toFile(), "libs");
        tempLibs.mkdirs();
        tempLibs.deleteOnExit();

        File someJar1 = new File(tempLibs, "thermostat-osgi-fluff1.jar");
        someJar1.createNewFile();
        someJar1.deleteOnExit();
        someJarName1 = someJar1.toPath();
        
        File someJar2 = new File(tempLibs, "thermostat-osgi-fluff2.jar");
        someJar2.createNewFile();
        someJar2.deleteOnExit();
        someJarName2 = someJar2.toPath();

        File missingJar = new File(tempLibs, "thisjar_noexist.jar");
        missingJarName = missingJar.toPath();
    }

    private String resolvedJar(Path jar) {
        return "file:" + jar.toString();
    }

    @Test
    public void verifyGetName() {
        Properties props = new Properties();
        String name = "name";
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, "");

        String commandName = info.getName();
        assertEquals(name, commandName);
    }

    @Test
    public void verifySingleResource() {
        Properties props = new Properties();
        props.setProperty("bundles", someJarName1.getFileName().toString());
        String name = "name";
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(1, resources.size());
        assertTrue(resources.contains(resolvedJar(someJarName1)));
    }

    @Test
    public void verifyMultipleResources() {
        Properties props = new Properties();
        props.setProperty("bundles", someJarName1.getFileName() + "," + someJarName2.getFileName());
        String name = "name";
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resolvedJar(someJarName1)));
        assertTrue(resources.contains(resolvedJar(someJarName2)));
    }

    @Test
    public void verifyMissingResource() {
        Properties props = new Properties();
        props.setProperty("bundles", missingJarName.getFileName().toString());
        String name = "name";
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(0, resources.size());
        assertFalse(resources.contains(resolvedJar(missingJarName)));
    }

    @Test
    public void verifyGetDescription() {
        Properties props = new Properties();
        String name = "name";
        String desc = "desc";
        props.put("description", desc);
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        String commandDesc = info.getDescription();
        assertEquals(desc, commandDesc);
    }

    @Test
    public void verifyGetUsage() {
        Properties props = new Properties();
        String name = "name";
        String usage = "some sort of usage message";
        props.put("usage", usage);
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        String commandUsage = info.getUsage();
        assertEquals(usage, commandUsage);
    }

    @Test
    public void verifyGetOptions() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "foo, bar");
        props.put("foo.short", "f");
        props.put("foo.long", "foo");
        props.put("foo.hasarg", "true");
        props.put("foo.required", "TRUE");
        props.put("foo.description", "the foo option");
        props.put("bar.short", "b");
        props.put("bar.long", "bar");
        props.put("bar.hasarg", "FALSE");
        props.put("bar.required", "this will evaluate as false");
        props.put("bar.description", "the bar option");
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        Options options = info.getOptions();
        Option foo = options.getOption("foo");
        assertEquals("foo", foo.getArgName());
        assertEquals("f", foo.getOpt());
        assertEquals("foo", foo.getLongOpt());
        assertTrue(foo.hasArg());
        assertTrue(foo.isRequired());
        assertEquals("the foo option", foo.getDescription());
        Option bar = options.getOption("bar");
        assertEquals("bar", bar.getArgName());
        assertEquals("b", bar.getOpt());
        assertEquals("bar", bar.getLongOpt());
        assertFalse(bar.hasArg());
        assertFalse(bar.isRequired());
        assertEquals("the bar option", bar.getDescription());
    }
    
    @Test
    public void canAddCommonDBOptions() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_DB_OPTIONS");
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        Options options = info.getOptions();
        assertTrue(options.hasOption(CommonOptions.DB_URL_ARG));
        assertTrue(options.hasOption(CommonOptions.USERNAME_ARG));
        assertTrue(options.hasOption(CommonOptions.PASSWORD_ARG));
        assertFalse(options.getOption(CommonOptions.DB_URL_ARG).isRequired());
        Option dbUrlOption = options.getOption(CommonOptions.DB_URL_ARG);
        Translate<LocaleResources> t = LocaleResources.createLocalizer();
        assertEquals(t.localize(LocaleResources.OPTION_DB_URL_DESC), dbUrlOption.getDescription());
        assertEquals("d", dbUrlOption.getOpt());
        assertEquals("dbUrl", dbUrlOption.getLongOpt());
    }
    
    @Test
    public void requiredCommandPropertyOverridesCommonDbOptions() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_DB_OPTIONS, dbUrl");
        props.put("dbUrl.required", "true");
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        Options options = info.getOptions();
        assertTrue(options.hasOption(CommonOptions.DB_URL_ARG));
        Option dbUrlOption = options.getOption(CommonOptions.DB_URL_ARG);
        assertTrue(dbUrlOption.isRequired());
        assertEquals("dbUrl", dbUrlOption.getLongOpt());
    }
    
    @Test
    public void canAddLogOption() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_LOG_OPTION");
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        Options options = info.getOptions();
        assertTrue(options.hasOption(CommonOptions.LOG_LEVEL_ARG));
        assertFalse(options.getOption(CommonOptions.LOG_LEVEL_ARG).isRequired());
    }

    @Test
    public void verifyOptionGroup() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "foo|bar");
        props.put("foo.short", "f");
        props.put("bar.short", "b");
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());

        Options options = info.getOptions();
        Option foo = options.getOption("f");
        assertNotNull(foo);
        OptionGroup og = options.getOptionGroup(foo);
        assertNotNull(og);
        Option bar = options.getOption("b");
        @SuppressWarnings("rawtypes")
        Collection members = og.getOptions();
        assertTrue(members.contains(foo));
        assertTrue(members.contains(bar));
    }

    @Test(expected=RuntimeException.class)
    public void verifyConflictingShortOptions() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "bar,baz");
        props.put("bar.short", "b");
        props.put("baz.short", "b");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyConflictingLongOptions() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "bar,baz");
        props.put("bar.long", "ba");
        props.put("baz.long", "ba");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyConflictingOptionsInGroup() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "bar|baz");
        props.put("bar.short", "b");
        props.put("baz.short", "b");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyGroupOptionConflictingWithNongroupOption() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "foo|bar,baz");
        props.put("foo.short", "f");
        props.put("bar.short", "b");
        props.put("baz.short", "b");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyConflictsWithCommonShortOption() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_DB_OPTIONS, dbUrl");
        props.put("dbUrl.short", "x");
        props.put("dbUrl.long", "dbUrl");
        props.put("dbUrl.required", "true");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyConflictsWithCommonLongOption() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_DB_OPTIONS, dbUrl");
        props.put("dbUrl.short", "d");
        props.put("dbUrl.long", "notDbUrl");
        props.put("dbUrl.required", "true");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }

    @Test(expected=RuntimeException.class)
    public void verifyDescriptionConflictsWithCommonOption() {
        Properties props = new Properties();
        String name = "name";
        props.put("options", "AUTO_DB_OPTIONS, dbUrl");
        props.put("dbUrl.description", "An attempt to cause confusion.");
        props.put("dbUrl.required", "true");
        @SuppressWarnings("unused")
        BuiltInCommandInfo info = new BuiltInCommandInfo(name, props, tempLibs.toString());
    }
}
