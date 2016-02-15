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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.Configurations;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.PluginID;
import com.redhat.thermostat.plugin.validator.PluginConfigurationValidatorException;

public class PluginInfoSourceTest {

    private Path testRoot;
    private Path jarRootDir;
    private Path sysPluginRootDir;
    private Path userPluginRootDir;
    private Path sysConfRootDir;
    private Path userConfRootDir;
    private Path userConfCustomDir;
    private PluginConfigurationParser parser;
    private PluginConfiguration parserResult;
    private UsageStringBuilder usageBuilder;

    @Before
    public void setUp() throws IOException, PluginConfigurationValidatorException {
        parser = mock(PluginConfigurationParser.class);
        parserResult = mock(PluginConfiguration.class);
        when(parser.parse(isA(File.class))).thenReturn(parserResult);
        usageBuilder = mock(UsageStringBuilder.class);

        testRoot = Files.createTempDirectory("thermostat");
        sysPluginRootDir = testRoot.resolve("plugins");
        Files.createDirectory(sysPluginRootDir);
        userPluginRootDir = testRoot.resolve("plugins-user");
        Files.createDirectory(userPluginRootDir);
        jarRootDir = testRoot.resolve("libs");
        Files.createDirectories(jarRootDir);
        sysConfRootDir = testRoot.resolve("sysconf");
        Files.createDirectories(sysConfRootDir);
        userConfRootDir = testRoot.resolve("userconf");
        Files.createDirectories(userConfRootDir);
        userConfCustomDir = testRoot.resolve("customconf");
        Files.createDirectories(userConfCustomDir);
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(testRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    @Test
    public void verifyParserIsInvokedOnAllConfigurationFiles() throws IOException, PluginConfigurationValidatorException {
        Path[] pluginDirs = new Path[] {
                sysPluginRootDir.resolve("plugin1"),
                sysPluginRootDir.resolve("plugin2"),
        };
        for (Path pluginDir : pluginDirs) {
            Files.createDirectory(pluginDir);
        }

        new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        ArgumentCaptor<File> configFilesCaptor = ArgumentCaptor.forClass(File.class);
        verify(parser, times(pluginDirs.length)).parse(configFilesCaptor.capture());

        List<File> configurationFiles = configFilesCaptor.getAllValues();
        assertEquals(pluginDirs.length, configurationFiles.size());
        for (int i = 0; i < pluginDirs.length; i++) {
            assertTrue(configurationFiles.contains(pluginDirs[i].resolve("thermostat-plugin.xml").toFile()));
        }
    }

    @Test
    public void verifyMissingConfigurationFileIsHandledCorrectly() throws FileNotFoundException, PluginConfigurationValidatorException {
        when(parser.parse(isA(File.class))).thenThrow(new FileNotFoundException("test"));

        new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);
    }

    @Test(expected = CommandInfoNotFoundException.class)
    public void verifyMissingCommandInfo() {
        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);
        source.getCommandInfo("TEST");
    }

    @Test
    public void verifyCommandInfoObjectsToExtendExistingCommandsAreCreated() throws IOException {
        BundleInformation bundleInfo = new BundleInformation("plugin-bundle", "0.1");

        Path pluginDir = sysPluginRootDir.resolve("plugin1");
        Files.createDirectory(pluginDir);

        CommandExtensions extensions = mock(CommandExtensions.class);
        when(extensions.getCommandName()).thenReturn("command-name");
        when(extensions.getBundles()).thenReturn(Arrays.asList(bundleInfo));

        when(parserResult.getExtendedCommands()).thenReturn(Arrays.asList(extensions));

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);
        CommandInfo info = source.getCommandInfo("command-name");
        assertEquals("command-name", info.getName());

        assertTrue(info.getBundles().contains(bundleInfo));
    }

    @Test
    public void verifyCommandInfoObjectsForNewComamndsAreCreated() throws IOException {
        final String NAME = "command-name";
        final String DESCRIPTION = "description of the command";
        final String USAGE = "usage";
        final Options OPTIONS = new Options();
        final Set<Environment> ENVIRONMENTS = EnumSet.of(Environment.SHELL);
        BundleInformation bundleInfo = new BundleInformation("plugin-bundle", "0.1");

        Path pluginDir = sysPluginRootDir.resolve("plugin1");
        Files.createDirectory(pluginDir);

        NewCommand cmd = mock(NewCommand.class);
        when(cmd.getCommandName()).thenReturn(NAME);
        when(cmd.getDescription()).thenReturn(DESCRIPTION);
        when(usageBuilder.getUsage(NAME, OPTIONS)).thenReturn(USAGE);
        when(cmd.getOptions()).thenReturn(OPTIONS);
        when(cmd.getEnvironments()).thenReturn(ENVIRONMENTS);
        when(cmd.getBundles()).thenReturn(Arrays.asList(bundleInfo));

        when(parserResult.getNewCommands()).thenReturn(Arrays.asList(cmd));

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        CommandInfo result = source.getCommandInfo(NAME);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(USAGE, result.getUsage());
        assertEquals(OPTIONS, result.getOptions());

        List<BundleInformation> deps = result.getBundles();
        assertEquals(1, deps.size());
        assertTrue(deps.contains(bundleInfo));
    }

    @Test
    public void testConfigurationSysConfig() throws IOException {
        String pluginID = "com.redhat.thermostat.sys";
        String configName = "config.conf";

        String sysConfString = "key1=value1";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(sysConfRootDir, pluginID, configName, sysConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.containsKey("key1"));
        assertTrue(confMap.containsValue("value1"));
    }

    @Test
    public void testConfigurationUserConfig() throws IOException {
        String pluginID = "com.redhat.thermostat.user";
        String configName = "config.conf";

        String userConfString = "key1=value1";

        Path pluginDir = userPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(userConfRootDir, pluginID, configName, userConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.containsKey("key1"));
        assertTrue(confMap.containsValue("value1"));
    }

    @Test
    public void testConfigurationKeyOverlap() throws IOException {
        String pluginID = "com.redhat.thermostat.keyOverlap";
        String configName = "config.conf";

        String sysConfString = "key1=sys1\n";
        String userConfString = "key1=user1\n";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(sysConfRootDir, pluginID, configName, sysConfString);
        createTempDirAndFile(userConfRootDir, pluginID, configName, userConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.containsKey("key1"));

        assertTrue(confMap.containsValue("user1"));
        assertTrue(!confMap.containsValue("sys1"));

    }

    @Test
    public void testConfigurationMultipleConfigs() throws IOException {
        String pluginID = "com.redhat.thermostat.multipleConfig";
        String configOne = "one.conf";
        String configTwo = "two.conf";

        String sysConfStringOne = "one1=sys1\n";
        String userConfStringOne = "one2=user1\n";

        String sysConfStringTwo = "two1=sys1\n";
        String userConfStringTwo = "two2=user1\n";

        createTempDirAndFile(sysConfRootDir, pluginID, configOne, sysConfStringOne);
        createTempDirAndFile(userConfRootDir, pluginID, configOne, userConfStringOne);

        createTempDirAndFile(sysConfRootDir, pluginID, configTwo, sysConfStringTwo);
        createTempDirAndFile(userConfRootDir, pluginID, configTwo, userConfStringTwo);

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMapOne = source.getConfiguration(pluginID, configOne);
        Map<String, String> confMapTwo = source.getConfiguration(pluginID, configTwo);

        assertTrue(confMapOne.containsKey("one1"));
        assertTrue(confMapOne.containsKey("one2"));

        assertTrue(confMapTwo.containsKey("two1"));
        assertTrue(confMapTwo.containsKey("two2"));

    }

    @Test
    public void testConfigurationEmptyOverlap() throws IOException {
        String pluginID = "com.redhat.thermostat.emptyOverlap";
        String configName = "config.conf";

        String sysConfString = "key1=value1\n";
        String userConfString = "";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(sysConfRootDir, pluginID, configName, sysConfString);
        createTempDirAndFile(userConfRootDir, pluginID, configName, userConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.containsKey("key1"));
        assertTrue(confMap.containsValue("value1"));
    }
    @Test
    public void testConfigurationCustomPath() throws IOException {
        String pluginID = "com.redhat.thermostat.custom";

        String configName = "config.conf";
        String confString = "key1=value1";

        Path confPath = createTempConfigFile(userConfCustomDir, configName, confString);
        String customConfigPath = confPath.toAbsolutePath().toString();

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        Map<String, String> conf = new HashMap<String, String>();
        conf.put(configName, customConfigPath);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.getConfigurations()).thenReturn(new Configurations(conf));
        when(parserResult.hasValidID()).thenReturn(true);


        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);
        assertTrue(confMap.containsKey("key1"));
    }

    @Test
    public void testConfigurationDoesNotExist() throws IOException {
        String pluginID = "com.redhat.thermostat.emptyOverlap";
        String configName = "config.conf";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.isEmpty());
    }

    @Test
    public void testValuelessConfiguration() throws IOException {
        String pluginID = "com.redhat.thermostat.simple";
        String configName = "config.conf";

        String sysConfString = "keywithnovalue";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(sysConfRootDir, pluginID, configName, sysConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.containsKey(sysConfString));
        assertEquals("", confMap.get(sysConfString));
    }

    @Test
    public void testCommentInConfiguration() throws IOException {
        String pluginID = "com.redhat.thermostat.simple";
        String configName = "config.conf";

        String sysConfString = "#comment";

        Path pluginDir = sysPluginRootDir.resolve(pluginID);
        Files.createDirectory(pluginDir);

        when(parserResult.getPluginID()).thenReturn(new PluginID(pluginID));
        when(parserResult.hasValidID()).thenReturn(true);

        createTempDirAndFile(sysConfRootDir, pluginID, configName, sysConfString);

        PluginInfoSource source = new PluginInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(),
                userPluginRootDir.toFile(), sysConfRootDir.toFile(), userConfRootDir.toFile(),
                parser, usageBuilder);

        Map<String, String> confMap = source.getConfiguration(pluginID, configName);

        assertTrue(confMap.isEmpty());
    }

    private Path createTempDirAndFile(Path dir, String pluginID, String configName, String confString) throws IOException {
        Path confDir = dir.resolve(pluginID);
        Files.createDirectories(confDir);

        return createTempConfigFile(confDir, configName, confString);
    }

    private Path createTempConfigFile(Path dir, String configName, String confString) throws IOException {
        Path Conf = dir.resolve(configName);
        Files.write(Conf, confString.getBytes());
        assertTrue(Conf.toFile().exists());

        return Conf;
    }
}

