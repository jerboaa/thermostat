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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VmCriterionTest {

    @Test
    public void testJavaVersion() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.JAVA_VERSION;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setJavaVersion("1.8");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "1.8"), is(true));
        assertThat(criterion.match(matchContext, "1.80"), is(false));
        assertThat(criterion.match(matchContext, "1.8_u45"), is(false));
        assertThat(criterion.match(matchContext, "1.7"), is(false));
        assertThat(criterion.match(matchContext, "1."), is(false));
        assertThat(criterion.match(matchContext, "1"), is(false));
        assertThat(criterion.match(matchContext, "8"), is(false));
    }

    @Test
    public void testMainClass() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.MAINCLASS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setMainClass("com.example.java.ExampleApplet");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "com.example.java.ExampleApplet"), is(true));
        assertThat(criterion.match(matchContext, "com.example.java.ExampleApplet.*"), is(true));
        assertThat(criterion.match(matchContext, "com.example.java.ExampleApplet.+"), is(false));
        assertThat(criterion.match(matchContext, "com.example.java.Example.+"), is(true));
        assertThat(criterion.match(matchContext, "ExampleApplet"), is(true));
        assertThat(criterion.match(matchContext, ".*Applet"), is(true));
        assertThat(criterion.match(matchContext, ".*Applet.+"), is(false));
        assertThat(criterion.match(matchContext, ".*"), is(true));
        assertThat(criterion.match(matchContext, "*"), is(false));
        assertThat(criterion.match(matchContext, "com.example.java"), is(true));
        assertThat(criterion.match(matchContext, "com.example.java..*"), is(true));
        assertThat(criterion.match(matchContext, "com.example.java.ExampleApplet2"), is(false));
    }

    @Test
    public void testVmStatusWithRunningVM() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(-1l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(-1l);
        agentInfo.setAlive(true);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        assertThat(criterion.match(matchContext, "RUNNING"), is(true));
        assertThat(criterion.match(matchContext, "RuNnInG"), is(true));
        assertThat(criterion.match(matchContext, "running"), is(true));
        assertThat(criterion.match(matchContext, "EXITED"), is(false));
        assertThat(criterion.match(matchContext, "ExItEd"), is(false));
        assertThat(criterion.match(matchContext, "exited"), is(false));
        assertThat(criterion.match(matchContext, "UNKNOWN"), is(false));
        assertThat(criterion.match(matchContext, "UnKnOwN"), is(false));
        assertThat(criterion.match(matchContext, "unknown"), is(false));
    }

    @Test
    public void testVmStatusWithExitedVM() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(150l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(-1l);
        agentInfo.setAlive(true);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        assertThat(criterion.match(matchContext, "RUNNING"), is(false));
        assertThat(criterion.match(matchContext, "RuNnInG"), is(false));
        assertThat(criterion.match(matchContext, "running"), is(false));
        assertThat(criterion.match(matchContext, "EXITED"), is(true));
        assertThat(criterion.match(matchContext, "ExItEd"), is(true));
        assertThat(criterion.match(matchContext, "exited"), is(true));
        assertThat(criterion.match(matchContext, "UNKNOWN"), is(false));
        assertThat(criterion.match(matchContext, "UnKnOwN"), is(false));
        assertThat(criterion.match(matchContext, "unknown"), is(false));
    }

    @Test
    public void testVmStatusWithExitedVMAndDeadAgent() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(150l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(125l);
        agentInfo.setAlive(false);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        assertThat(criterion.match(matchContext, "RUNNING"), is(false));
        assertThat(criterion.match(matchContext, "RuNnInG"), is(false));
        assertThat(criterion.match(matchContext, "running"), is(false));
        assertThat(criterion.match(matchContext, "EXITED"), is(true));
        assertThat(criterion.match(matchContext, "ExItEd"), is(true));
        assertThat(criterion.match(matchContext, "exited"), is(true));
        assertThat(criterion.match(matchContext, "UNKNOWN"), is(false));
        assertThat(criterion.match(matchContext, "UnKnOwN"), is(false));
        assertThat(criterion.match(matchContext, "unknown"), is(false));
    }

    @Test
    public void testVmStatusWithRunningVMAndDeadAgent() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(-1l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(125l);
        agentInfo.setAlive(false);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        assertThat(criterion.match(matchContext, "RUNNING"), is(false));
        assertThat(criterion.match(matchContext, "RuNnInG"), is(false));
        assertThat(criterion.match(matchContext, "running"), is(false));
        assertThat(criterion.match(matchContext, "EXITED"), is(false));
        assertThat(criterion.match(matchContext, "ExItEd"), is(false));
        assertThat(criterion.match(matchContext, "exited"), is(false));
        assertThat(criterion.match(matchContext, "UNKNOWN"), is(true));
        assertThat(criterion.match(matchContext, "UnKnOwN"), is(true));
        assertThat(criterion.match(matchContext, "unknown"), is(true));
    }

    @Test(expected = UnrecognizedArgumentException.class)
    public void testVmStatusDoesNotAcceptSubstrings() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(-1l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(-1l);
        agentInfo.setAlive(true);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        criterion.match(matchContext, "RUN");
    }

    @Test(expected = UnrecognizedArgumentException.class)
    public void testVmStatusDoesNotAcceptSubstrings2() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(-1l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(-1l);
        agentInfo.setAlive(true);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        criterion.match(matchContext, "EXIT");
    }


    @Test(expected = UnrecognizedArgumentException.class)
    public void testVmStatusDoesNotAcceptSubstrings3() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_STATUS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setStartTimeStamp(100l);
        vmInfo.setStopTimeStamp(-1l);
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(50l);
        agentInfo.setStopTime(-1l);
        agentInfo.setAlive(true);
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).agentInfo(agentInfo).build();
        criterion.match(matchContext, "UNK");
    }

    @Test
    public void testVmName() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_NAME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmName("Example JVM Implementation Name");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "Example JVM Implementation Name"), is(true));
        assertThat(criterion.match(matchContext, "Example JVM Implementation Name 2"), is(false));
        assertThat(criterion.match(matchContext, "JVM Implementation"), is(false));
        assertThat(criterion.match(matchContext, "Example JVM"), is(false));
        assertThat(criterion.match(matchContext, "*"), is(false));
    }

    @Test
    public void testVmArgs() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_ARGS;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmArguments("-Xmx1024M -Xms1024M");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "-Xmx1024M -Xms1024M"), is(true));
        assertThat(criterion.match(matchContext, "-Xmx1024M,-Xms1024M"), is(true));
        assertThat(criterion.match(matchContext, "-Xms1024M,-Xmx1024M"), is(true));
        assertThat(criterion.match(matchContext, "-Xmx1024M"), is(true));
        assertThat(criterion.match(matchContext, "-Xms1024M"), is(true));
        assertThat(criterion.match(matchContext, "Xmx1024M"), is(true));
        assertThat(criterion.match(matchContext, "Xmx"), is(true));
        assertThat(criterion.match(matchContext, "Xms,Xmx"), is(true));
        assertThat(criterion.match(matchContext, "-Xmx2048M"), is(false));
    }

    @Test
    public void testVmVersion() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.VM_VERSION;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmVersion("20.5.6");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "20.5.6"), is(true));
        assertThat(criterion.match(matchContext, "20.5,6"), is(false));
        assertThat(criterion.match(matchContext, "20.5.8"), is(false));
        assertThat(criterion.match(matchContext, "21.5.6"), is(false));
        assertThat(criterion.match(matchContext, "20"), is(false));
        assertThat(criterion.match(matchContext, "20+"), is(false));
        assertThat(criterion.match(matchContext, "<=21"), is(false));
    }

    @Test
    public void testUsername() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.USERNAME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setUsername("foo-user");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "foo-user"), is(true));
        assertThat(criterion.match(matchContext, "foo"), is(false));
        assertThat(criterion.match(matchContext, "user"), is(false));
        assertThat(criterion.match(matchContext, "*"), is(false));
        assertThat(criterion.match(matchContext, ".*"), is(false));
    }

    @Test
    public void testJavahome() throws UnrecognizedArgumentException {
        VmCriterion criterion = VmCriterion.JAVA_HOME;
        VmInfo vmInfo = new VmInfo();
        vmInfo.setJavaHome("/some/filesystem/path/to/jdk");
        MatchContext matchContext = MatchContext.builder().vmInfo(vmInfo).build();
        assertThat(criterion.match(matchContext, "/some/filesystem/path/to/jdk"), is(true));
        assertThat(criterion.match(matchContext, "/some/filesystem/path"), is(false));
        assertThat(criterion.match(matchContext, "/some/filesystem/path/to/"), is(false));
        assertThat(criterion.match(matchContext, "/some/filesystem/path/to/jdk/"), is(true));
        assertThat(criterion.match(matchContext, "some/filesystem/path/to/jdk/"), is(false));
        assertThat(criterion.match(matchContext, "/other/some/filesystem/path/to/jdk/"), is(false));
        assertThat(criterion.match(matchContext, "/some/filesystem/path"), is(false));
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
