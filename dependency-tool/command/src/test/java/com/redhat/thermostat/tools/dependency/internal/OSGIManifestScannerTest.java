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

package com.redhat.thermostat.tools.dependency.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 */
public class OSGIManifestScannerTest {

    private static final String Export_Package =
            "org.jboss.netty.handler.codec.serialization;" +
            "uses:=\"org.jboss.netty.buffer,org.jboss.netty.handler." +
            "codec.replay,org.jboss.netty.channel,org.jboss.netty.handler." +
            "codec.oneone,org.jboss.netty.handler.codec." +
            "frame\";version=\"3.2.4.Final\",org.jboss.netty." +
            "util;uses:=\"org.jboss.netty.buffer,org.jboss.netty.channel," +
            "org.jboss.netty.logging\";version=\"3.2.4.Final\",org.jboss." +
            "netty.handler.codec.compression;uses:=\"org.jboss.netty." +
            "buffer,org.jboss.netty.channel,org.jboss.netty.handler." +
            "codec.oneone\";version=\"3.2.4.Final\",org.jboss.netty." +
            "handler.execution;uses:=\"org.jboss.netty.channel,org." +
            "jboss.netty.util,org.jboss.netty." +
            "logging\";version=\"3.2.4.Final\",org.jboss.netty.channel." +
            "local;uses:=\"org.jboss.netty.channel,org.jboss.netty." +
            "logging\";version=\"3.2.4.Final\",org.jboss.netty.bootstrap" +
            ";uses:=\"org.jboss.netty.channel,org.jboss.netty." +
            "util\";version=\"3.2.4.Final\",org.jboss.netty.handler.codec." +
            "base64;uses:=\"org.jboss.netty.buffer,org.jboss.netty.channel," +
            "org.jboss.netty.handler.codec.oneone,org.jboss.netty" +
            ".util\";version=\"3.2.4.Final\",org.jboss.netty.handler." +
            "timeout;uses:=\"org.jboss.netty.channel,org.jboss.netty." +
            "util\";version=\"3.2.4.Final\",org.jboss.netty.channel.socket." +
            "nio;uses:=\"org.jboss.netty.logging,org.jboss.netty.channel." +
            "socket,org.jboss.netty.channel,org.jboss.netty.util,org.jboss." +
            "netty.buffer\";version=\"3.2.4.Final\",org.jboss.netty.handler" +
            ".codec.http.websocket;uses:=\"org.jboss.netty.buffer,org.jboss." +
            "netty.util,org.jboss.netty.handler.codec.replay,org.jboss.netty" +
            ".handler.codec.frame,org.jboss.netty.channel,org.jboss.netty." +
            "handler.codec.oneone\";version=\"3.2.4.Final\",org.jboss." +
            "netty.handler.codec.replay;uses:=\"org.jboss.netty.buffer," +
            "org.jboss.netty.channel\";version=\"3.2.4.Final\",org.jboss." +
            "netty.handler.codec.string;uses:=\"org.jboss.netty.buffer," +
            "org.jboss.netty.channel,org.jboss.netty.handler.codec." +
            "oneone\";version=\"3.2.4.Final\",org.jboss.netty.channel;" +
            "uses:=\"org.jboss.netty.buffer,org.jboss.netty.util,org.jboss" +
            ".netty.logging\";version=\"3.2.4.Final\",org.jboss.netty.handler." +
            "ssl;uses:=\"org.jboss.netty.channel,javax.net.ssl,org.jboss." +
            "netty.logging,org.jboss.netty.buffer,org.jboss.netty.handler." +
            "codec.frame\";version=\"logging\";version=\"3.2.4.Final\",org." +
            "jboss.netty.container.microcontainer;uses:=\"org.jboss.netty." +
            "logging\";version=\"3.2.4.Final\",org.jboss.netty.handler." +
            "codec.rtsp;uses:=\"org.jboss.netty.buffer,org.jboss.netty." +
            "channel,org.jboss.netty.handler.codec.http,org.jboss.netty." +
            "handler.codec.embedder\";version=\"3.2.4.Final\",org.jboss." +
            "netty.handler.codec.protobuf;uses:=\"org.jboss.netty.buffer," +
            "org.jboss.netty.channel,org.jboss.netty.handler.codec.oneone," +
            "com.google.protobuf,org.jboss.netty.handler.codec." +
            "frame\";version=\"3.2.4.Final\",org.jboss.netty.channel.socket." +
            "http;uses:=\"org.jboss.netty.channel,org.jboss.netty.channel." +
            "socket,org.jboss.netty.handler.codec.http,javax.net.ssl,org." +
            "jboss.netty.handler.ssl,org.jboss.netty.buffer,org.jboss." +
            "netty.logging,javax.servlet,org.jboss.netty.channel.local," +
            "javax.servlet.http\";version=\"3.2.4.Final\",org.jboss.netty." +
            "channel.group;uses:=\"org.jboss.netty.channel,org.jboss.netty." +
            "buffer,org.jboss.netty.logging\";version=\"3.2.4.Final\",org." +
            "jboss.netty.handler.codec.embedder;uses:=\"org.jboss.netty." +
            "channel,org.jboss.netty.buffer\";version=\"3.2.4.Final\",org." +
            "jboss.netty.channel.socket.oio;uses:=\"org.jboss.netty." +
            "channel,org.jboss.netty.channel.socket,org.jboss.netty.util," +
            "org.jboss.netty.buffer,org.jboss.netty." +
            "logging\";version=\"3.2.4.Final\",org.jboss.netty.handler.codec." +
            "frame;uses:=\"org.jboss.netty.buffer,org.jboss.netty.channel," +
            "org.jboss.netty.handler.codec.oneone\";version=\"3.2.4." +
            "Final\",org.jboss.netty.handler.codec.oneone;uses:=\"org.jboss." +
            "netty.channel\";version=\"3.2.4.Final\",org.jboss.netty." +
            "container.osgi;uses:=\"org.jboss.netty.logging,org.osgi." +
            "framework\";version=\"3.2.4.Final\",org.jboss.netty." +
            "logging;uses:=\"org.apache.commons.logging,org.jboss.logging," +
            "org.apache.log4j,org.osgi.service.log,org.osgi.util.tracker," +
            "org.osgi.framework,org.slf4j\";version=\"3.2.4.Final\",org." +
            "jboss.netty.buffer;uses:=\"org.jboss.netty.util\";version" +
            "=\"3.2.4 .Final\",org.jboss.netty.handler.codec.http;" +
            "uses:=\"org.jboss.netty.buffer,org.jboss.netty.handler.codec." +
            "frame,org.jboss.netty.channel,org.jboss.netty.util,org.jboss." +
            "netty.handler.codec.compression,org.jboss.netty.handler.codec" +
            ".embedder,org.jboss.netty.handler.codec.replay,org.jboss.netty" +
            ".handler.codec.oneone\";version=\"3.2.4.Final\",org.jboss.netty" +
            ".handler.queue;uses:=\"org.jboss.netty.channel,org.jboss.netty" +
            ".buffer\";version=\"3.2.4.Final\",org.jboss.netty.channel.socket" +
            ";uses:=\"org.jboss.netty.channel\";version=\"3.2.4.Final\",org." +
            "jboss.netty.handler.logging;uses:=\"org.jboss.netty.logging," +
            "org.jboss.netty.buffer,org.jboss.netty.channel\";version=" +
            "\"3.2.4.Final\",org.jboss.netty.handler.stream;uses:=\"org." +
            "jboss.netty.buffer,org.jboss.netty.channel,org.jboss.netty." +
            "logging\";version=\"3.2.4.Final\"";

    @Test
    public void testParseHeader() throws Exception {
        List<Dependency> packages = OSGIManifestScanner.parseHeader(Export_Package);
        Assert.assertEquals(31, packages.size());
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.serialization", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.util", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.compression", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.execution", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.local", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.bootstrap", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.base64", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.timeout", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.socket.nio", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.http.websocket", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.replay", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.string", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.ssl", "logging")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.container.microcontainer", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.rtsp", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.protobuf", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.socket.http", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.group", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.embedder", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.socket.oio", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.frame", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.oneone", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.container.osgi", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.logging", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.buffer", "3.2.4 .Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.codec.http", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.queue", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.channel.socket", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.logging", "3.2.4.Final")));
        Assert.assertTrue(packages.contains(new Dependency("org.jboss.netty.handler.stream", "3.2.4.Final")));
    }

    @Test
    public void testVersionExtractor() {
        String versionRange = "[9.1.3,10.1.2)";
        String[] extracted = OSGIManifestScanner.parseVersionRange(versionRange);
        Assert.assertEquals("9.1.3", extracted[0]);
        Assert.assertEquals("10.1.2", extracted[1]);
    }

    @Test
    public void testVersionMatcher() {
        String versionRange = "[3,4)";
        Dependency result = OSGIManifestScanner.parseAndCheckBounds(versionRange, "3.1.2", new Dependency("TestBundle", "3.1.2"));
        Assert.assertEquals(result, new Dependency("TestBundle", "3.1.2"));
    }

    @Test
    public void testVersionMatcher2() {
        String versionRange = "[3,4)";
        Dependency result = OSGIManifestScanner.parseAndCheckBounds(versionRange, "4", new Dependency("TestBundle", "4"));
        Assert.assertEquals(result, null);
    }

    @Test
    public void testVersionMatcher3() {
        String versionRange = "[5,5.1.2]";
        Dependency result = OSGIManifestScanner.parseAndCheckBounds(versionRange, "5.1.2", new Dependency("TestBundle", "5.1.2"));
        Assert.assertEquals(result, new Dependency("TestBundle", "5.1.2"));
    }

    @Test
    public void testNullDependency() {
        String versionRange = "[1,2]";
        Dependency result = OSGIManifestScanner.parseAndCheckBounds(versionRange, null, null);
        Assert.assertEquals(result, null);
    }

}
