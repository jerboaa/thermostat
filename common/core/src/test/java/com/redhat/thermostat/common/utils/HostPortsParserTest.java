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

package com.redhat.thermostat.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class HostPortsParserTest {

    @Test
    public void canParseIpV4Pair() throws IllegalArgumentException {
        HostPortsParser parser = new HostPortsParser(
                "127.0.0.1:8080,127.0.0.1:9999");
        parser.parse();
        List<HostPortPair> ipPorts = parser.getHostsPorts();
        assertEquals(2, ipPorts.size());
        assertEquals(8080, (long) ipPorts.get(0).getPort());
        assertEquals("127.0.0.1", ipPorts.get(0).getHost());
        assertEquals(9999, (long) ipPorts.get(1).getPort());
        assertEquals("127.0.0.1", ipPorts.get(1).getHost());
    }
    
    @Test
    public void canParseDnsHostsPortsPair() throws IllegalArgumentException {
        HostPortsParser parser = new HostPortsParser(
                "somehost.example.com:8080,host2.example.com:9999");
        parser.parse();
        List<HostPortPair> ipPorts = parser.getHostsPorts();
        assertEquals(2, ipPorts.size());
        assertEquals(8080, (long) ipPorts.get(0).getPort());
        assertEquals("somehost.example.com", ipPorts.get(0).getHost());
        assertEquals(9999, (long) ipPorts.get(1).getPort());
        assertEquals("host2.example.com", ipPorts.get(1).getHost());
        parser = new HostPortsParser(
                "thermostat-storage.fluff.org:9999");
        parser.parse();
        HostPortPair pair = parser.getHostsPorts().get(0);
        assertEquals("thermostat-storage.fluff.org", pair.getHost());
        assertEquals(9999, pair.getPort());
    }

    @Test
    public void canParseIpv6Pair() {
        HostPortsParser parser = new HostPortsParser(
                "[1fff:0:a88:85a3::ac1f]:8001,[1fff:0:a88:85a3::ac2f]:8001");
        parser.parse();
        List<HostPortPair> ipPorts = parser.getHostsPorts();
        assertEquals(2, ipPorts.size());
        assertEquals(8001, (long) ipPorts.get(0).getPort());
        assertEquals("1fff:0:a88:85a3::ac1f", ipPorts.get(0).getHost());
        assertEquals(8001, (long) ipPorts.get(1).getPort());
        assertEquals("1fff:0:a88:85a3::ac2f", ipPorts.get(1).getHost());
    }

    @Test
    public void failsParsingInvalidString() {
        HostPortsParser parser = new HostPortsParser(
                "1fff:0:a88:85a3::ac1f]:8001,[1fff:0:a88:85a3::ac2f]:8001");
        int expectedExcptns = 3;
        int exptns = 0;
        try {
            parser.parse();
        } catch (InvalidConfigurationException e) {
            exptns++;
        }
        parser = new HostPortsParser("blah,test");
        try {
            parser.parse();
        } catch (InvalidConfigurationException e) {
            exptns++;
        }
        parser = new HostPortsParser("127.0.0.1:80,127.0.0.2:bad");
        try {
            parser.parse();
        } catch (InvalidConfigurationException e) {
            exptns++;
        }
        assertEquals(expectedExcptns, exptns);
    }

    @Test(expected = IllegalStateException.class)
    public void getMapWithNoParseThrowsException() {
        HostPortsParser parser = new HostPortsParser("blah");
        parser.getHostsPorts();
    }
}

