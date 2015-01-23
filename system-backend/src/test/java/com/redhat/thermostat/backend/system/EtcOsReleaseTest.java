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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

import org.junit.Test;

import com.redhat.thermostat.test.Bug;

public class EtcOsReleaseTest {
    
    static final String NOT_EXISTING_OS_RELEASE_FILE = "/thermostat-os-release-testing-"
            + UUID.randomUUID();

    @Test
    public void testName() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new StringReader("NAME=\"Name\"\n"));
        DistributionInformation info = new EtcOsRelease().getFromOsRelease(reader);
        assertEquals("Name", info.getName());
    }


    @Test
    public void testVersion() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("VERSION=\"Version\"\n"));
        DistributionInformation info = new EtcOsRelease().getFromOsRelease(reader);
        assertEquals("Version", info.getVersion());
    }

    @Bug(id="981",
        summary="DistributionInformationTest fails on OpenSUSE Linux 12.1",
        url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=981")
    @Test
    public void testFormattedOutput() throws IOException {
        String output =
            "NAME=openSUSE\n" +
            "VERSION = 12.1 (Asparagus)\n" +
            "VERSION_ID=\"12.1\"\n" +
            "PRETTY_NAME=\"openSUSE 12.1 (Asparagus) (x86_64)\"\n" +
            "ID=opensuse";
        BufferedReader reader = new BufferedReader(new StringReader(output));
        DistributionInformation info = new EtcOsRelease().getFromOsRelease(reader);

        assertEquals("openSUSE", info.getName());
        assertEquals("12.1 (Asparagus)", info.getVersion());
    }
    
    @Test
    public void getDistributionInformationThrowsIOExceptionIfFileNotThere() {
        EtcOsRelease etcOsRelease = new EtcOsRelease(NOT_EXISTING_OS_RELEASE_FILE);
        try {
            etcOsRelease.getDistributionInformation();
            fail("Should have thrown IOException, since file is not there!");
        } catch (IOException e) {
            // pass
            String message = e.getMessage();
            assertTrue(message.contains("/thermostat-os-release-testing-"));
            assertTrue(message.contains("(No such file or directory)"));
        }
    }

}

