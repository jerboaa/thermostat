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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultsRendererTest {

    private static final HostInfo HOST_INFO = new HostInfo();
    private static final VmInfo VM_INFO = new VmInfo();
    private static final Pair<HostInfo, VmInfo> PAIR = new Pair<>(HOST_INFO, VM_INFO);

    static {
        HOST_INFO.setOsName("foo-os");
        HOST_INFO.setHostname("foo-host");
        HOST_INFO.setOsKernel("foo-kern");

        VM_INFO.setJavaHome("/some/path/to/jdk");
        VM_INFO.setUsername("foo-user");
        VM_INFO.setVmVersion("20.5.6");
        VM_INFO.setVmArguments("-Xmx2014M -Xms1024M");
        VM_INFO.setJavaVersion("1.8");
        VM_INFO.setMainClass("com.example.java.ExampleApplet");
        VM_INFO.setVmId("foo-id");
        VM_INFO.setVmName("Example JVM Implementation");
        VM_INFO.setVmPid(2);
    }

    @Test
    public void testVmId() {
        ResultsRenderer.Field field = ResultsRenderer.Field.VM_ID;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("foo-id"));
    }

    @Test
    public void testMainclass() {
        ResultsRenderer.Field field = ResultsRenderer.Field.MAINCLASS;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("com.example.java.ExampleApplet"));
    }

    @Test
    public void testVmName() {
        ResultsRenderer.Field field = ResultsRenderer.Field.VMNAME;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("Example JVM Implementation"));
    }

    @Test
    public void testJavaVersion() {
        ResultsRenderer.Field field = ResultsRenderer.Field.JAVAVERSION;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("1.8"));
    }

    @Test
    public void testVmVersion() {
        ResultsRenderer.Field field = ResultsRenderer.Field.VMVERSION;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("20.5.6"));
    }

    @Test
    public void testPid() {
        ResultsRenderer.Field field = ResultsRenderer.Field.PID;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("2"));
    }

    @Test
    public void testUsername() {
        ResultsRenderer.Field field = ResultsRenderer.Field.USERNAME;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("foo-user"));
    }

    @Test
    public void testHostname() {
        ResultsRenderer.Field field = ResultsRenderer.Field.HOSTNAME;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("foo-host"));
    }

    @Test
    public void testOsName() {
        ResultsRenderer.Field field = ResultsRenderer.Field.OSNAME;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("foo-os"));
    }

    @Test
    public void testOsKernel() {
        ResultsRenderer.Field field = ResultsRenderer.Field.OSKERNEL;
        String result = field.getAdaptedField(PAIR);
        assertThat(result, containsString("foo-kern"));
    }

    @Test
    public void testIsShortOutputDefault() {
        ResultsRenderer renderer = new ResultsRenderer(mock(Arguments.class));
        assertThat(renderer.isShortOutput(), is(true));
    }

    @Test
    public void testIsShortOutputNotOverriddenByVmIdAlone() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.VM_ID.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        assertThat(renderer.isShortOutput(), is(true));
    }

    @Test
    public void testIsShortOutputOverriddenByVmIdWithOthers() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.VM_ID.getCliSwitch())).thenReturn(true);
        when(arguments.hasArgument(ResultsRenderer.Field.HOSTNAME.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        assertThat(renderer.isShortOutput(), is(false));
    }

    @Test
    public void testIsShortOutputOverriddenByOthersWithoutVmId() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.PID.getCliSwitch())).thenReturn(true);
        when(arguments.hasArgument(ResultsRenderer.Field.HOSTNAME.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        assertThat(renderer.isShortOutput(), is(false));
    }

    @Test
    public void testIsShortOutputNotOverridenBySingleOtherSwitch() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.PID.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        assertThat(renderer.isShortOutput(), is(true));
    }

    @Test
    public void testGetHeaderFields() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.HOSTNAME.getCliSwitch())).thenReturn(true);
        when(arguments.hasArgument(ResultsRenderer.Field.PID.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        List<String> headers = renderer.getHeaderFields();
        assertThat(headers, is(equalTo(Arrays.asList(ResultsRenderer.Field.PID.name(), ResultsRenderer.Field.HOSTNAME.name()))));
    }

    @Test
    public void testGetInfo() {
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.HOSTNAME.getCliSwitch())).thenReturn(true);
        when(arguments.hasArgument(ResultsRenderer.Field.PID.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        List<String> info = renderer.getInfo(PAIR);
        assertThat(info, is(equalTo(Arrays.asList("2", "foo-host"))));
    }

    @Test
    public void testPrint() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Arguments arguments = mock(Arguments.class);
        when(arguments.hasArgument(ResultsRenderer.Field.HOSTNAME.getCliSwitch())).thenReturn(true);
        when(arguments.hasArgument(ResultsRenderer.Field.PID.getCliSwitch())).thenReturn(true);
        ResultsRenderer renderer = new ResultsRenderer(arguments);
        renderer.print(ps, Collections.singleton(PAIR));
        String output = baos.toString();
        assertThat(output, containsString("HOSTNAME"));
        assertThat(output, containsString("PID"));
        assertThat(output, containsString("foo-host"));
        assertThat(output, containsString("2"));
    }
}
