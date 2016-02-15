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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EnvironmentExecutorTest {
    
    private static final String FAKE_BIN_ROOT = "";
    private static final String[] ARGS = new String[] {
        "bar", "baz"
    };
    private static final String SCRIPT = "thermostat";
    
    @Test
    public void testExecutor() {
        Map<String, String> env = new HashMap<>();
        env.put("env1", "bar1");
        EnvironmentExecutor executor = new EnvironmentExecutor(FAKE_BIN_ROOT, SCRIPT, ARGS, env);
        assertEquals("thermostat bar baz", executor.toString());
        assertEquals(1, executor.getEnv().keySet().size());
        assertEquals("bar1", executor.getEnv().get("env1"));
    }
    
    @Test
    public void executeUpdatesEnvironment() throws IOException {
        String sharedEnvVar = "override-me-on-execute";
        Map<String, String> overrideEnv = new HashMap<>();
        final Map<String, String> processBuilderDefaultEnv = new HashMap<>();
        
        processBuilderDefaultEnv.put(sharedEnvVar, "old-value");
        processBuilderDefaultEnv.put("LANG", "C");
        overrideEnv.put(sharedEnvVar, "new-value");
        
        // Precondition
        assertTrue("expected processbuilder env *and* override env to contain " 
                   + sharedEnvVar,
                       overrideEnv.containsKey(sharedEnvVar) &&
                       processBuilderDefaultEnv.containsKey(sharedEnvVar));
        
        TestEnvironmentExecutor testExecutor = new TestEnvironmentExecutor(FAKE_BIN_ROOT, SCRIPT, ARGS, overrideEnv) {
            
            @Override
            protected Map<String, String> getBuilderEnvironment(ProcessBuilder builder) {
                return processBuilderDefaultEnv;
            }
        };
        
        // This should update the environment
        testExecutor.execute();
        assertTrue(testExecutor.executeCalled);
        
        Map<String, String> actualEnv = testExecutor.updatedEnvironment;
        assertEquals("env var " + sharedEnvVar + " should have been overridden",
                "new-value", actualEnv.get(sharedEnvVar));
        assertEquals("env var we didn't override should stay untouched",
                "C", actualEnv.get("LANG"));
    }
    
    private static class TestEnvironmentExecutor extends EnvironmentExecutor {
        
        TestEnvironmentExecutor(String binRoot, String script, String[] args,
                Map<String, String> env) {
            super(binRoot, script, args, env);
        }

        private Map<String, String> updatedEnvironment;
        private boolean executeCalled;
        
        @Override
        protected Process startProcess(ProcessBuilder builder) {
            executeCalled = true;
            return null;
        }
        
        @Override
        protected void updateEnvironment(Map<String, String> toUpdate) {
            super.updateEnvironment(toUpdate);
            updatedEnvironment = toUpdate;
        }
    }
}
