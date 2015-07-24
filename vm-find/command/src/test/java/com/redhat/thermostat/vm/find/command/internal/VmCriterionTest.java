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

import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VmCriterionTest {

    @Test
    public void testJavaVersion() {
        VmCriterion criterion = VmCriterion.JAVA_VERSION;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setJavaVersion("1.8");
        assertThat(criterion.match(vmInfo, "1.8"), is(true));
        assertThat(criterion.match(vmInfo, "1.80"), is(false));
        assertThat(criterion.match(vmInfo, "1.8_u45"), is(false));
        assertThat(criterion.match(vmInfo, "1.7"), is(false));
        assertThat(criterion.match(vmInfo, "1."), is(false));
        assertThat(criterion.match(vmInfo, "1"), is(false));
        assertThat(criterion.match(vmInfo, "8"), is(false));
    }

    @Test
    public void testMainClass() {
        VmCriterion criterion = VmCriterion.MAINCLASS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setMainClass("com.example.java.ExampleApplet");
        assertThat(criterion.match(vmInfo, "com.example.java.ExampleApplet"), is(true));
        assertThat(criterion.match(vmInfo, "com.example.java.ExampleApplet.*"), is(true));
        assertThat(criterion.match(vmInfo, "com.example.java.ExampleApplet.+"), is(false));
        assertThat(criterion.match(vmInfo, "com.example.java.Example.+"), is(true));
        assertThat(criterion.match(vmInfo, "ExampleApplet"), is(true));
        assertThat(criterion.match(vmInfo, ".*Applet"), is(true));
        assertThat(criterion.match(vmInfo, ".*Applet.+"), is(false));
        assertThat(criterion.match(vmInfo, ".*"), is(true));
        assertThat(criterion.match(vmInfo, "*"), is(false));
        assertThat(criterion.match(vmInfo, "com.example.java"), is(true));
        assertThat(criterion.match(vmInfo, "com.example.java..*"), is(true));
        assertThat(criterion.match(vmInfo, "com.example.java.ExampleApplet2"), is(false));
    }

    @Test
    public void testVmName() {
        VmCriterion criterion = VmCriterion.VM_NAME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmName("Example JVM Implementation Name");
        assertThat(criterion.match(vmInfo, "Example JVM Implementation Name"), is(true));
        assertThat(criterion.match(vmInfo, "Example JVM Implementation Name 2"), is(false));
        assertThat(criterion.match(vmInfo, "JVM Implementation"), is(false));
        assertThat(criterion.match(vmInfo, "Example JVM"), is(false));
        assertThat(criterion.match(vmInfo, "*"), is(false));
    }

    @Test
    public void testVmArgs() {
        VmCriterion criterion = VmCriterion.VM_ARGS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmArguments("-Xmx1024M -Xms1024M");
        assertThat(criterion.match(vmInfo, "-Xmx1024M -Xms1024M"), is(true));
        assertThat(criterion.match(vmInfo, "-Xmx1024M,-Xms1024M"), is(true));
        assertThat(criterion.match(vmInfo, "-Xms1024M,-Xmx1024M"), is(true));
        assertThat(criterion.match(vmInfo, "-Xmx1024M"), is(true));
        assertThat(criterion.match(vmInfo, "-Xms1024M"), is(true));
        assertThat(criterion.match(vmInfo, "Xmx1024M"), is(true));
        assertThat(criterion.match(vmInfo, "Xmx"), is(true));
        assertThat(criterion.match(vmInfo, "Xms,Xmx"), is(true));
        assertThat(criterion.match(vmInfo, "-Xmx2048M"), is(false));
    }

    @Test
    public void testVmVersion() {
        VmCriterion criterion = VmCriterion.VM_VERSION;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmVersion("20.5.6");
        assertThat(criterion.match(vmInfo, "20.5.6"), is(true));
        assertThat(criterion.match(vmInfo, "20.5,6"), is(false));
        assertThat(criterion.match(vmInfo, "20.5.8"), is(false));
        assertThat(criterion.match(vmInfo, "21.5.6"), is(false));
        assertThat(criterion.match(vmInfo, "20"), is(false));
        assertThat(criterion.match(vmInfo, "20+"), is(false));
        assertThat(criterion.match(vmInfo, "<=21"), is(false));
    }

    @Test
    public void testUsername() {
        VmCriterion criterion = VmCriterion.USERNAME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setUsername("foo-user");
        assertThat(criterion.match(vmInfo, "foo-user"), is(true));
        assertThat(criterion.match(vmInfo, "foo"), is(false));
        assertThat(criterion.match(vmInfo, "user"), is(false));
        assertThat(criterion.match(vmInfo, "*"), is(false));
        assertThat(criterion.match(vmInfo, ".*"), is(false));
    }

    @Test
    public void testJavahome() {
        VmCriterion criterion = VmCriterion.JAVA_HOME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setJavaHome("/some/filesystem/path/to/jdk");
        assertThat(criterion.match(vmInfo, "/some/filesystem/path/to/jdk"), is(true));
        assertThat(criterion.match(vmInfo, "/some/filesystem/path"), is(false));
        assertThat(criterion.match(vmInfo, "/some/filesystem/path/to/"), is(false));
        assertThat(criterion.match(vmInfo, "/some/filesystem/path/to/jdk/"), is(true));
        assertThat(criterion.match(vmInfo, "some/filesystem/path/to/jdk/"), is(false));
        assertThat(criterion.match(vmInfo, "/other/some/filesystem/path/to/jdk/"), is(false));
        assertThat(criterion.match(vmInfo, "/some/filesystem/path"), is(false));
    }

    @Test
    public void testFromStringWithValidArgument() {
        VmCriterion criterion = VmCriterion.fromString("javahome");
        assertThat(criterion, is(VmCriterion.JAVA_HOME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromStringWithInvalidArgument() {
        VmCriterion.fromString("not a valid criterion");
    }
}
