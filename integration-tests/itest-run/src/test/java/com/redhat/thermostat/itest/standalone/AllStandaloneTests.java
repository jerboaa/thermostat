/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.itest.standalone;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.redhat.thermostat.itest.CliTest;
import com.redhat.thermostat.itest.PluginTest;
import com.redhat.thermostat.itest.StorageConnectionTest;
import com.redhat.thermostat.itest.StorageTest;
import com.redhat.thermostat.itest.VmCommandsTest;


/**
 * <p>
 * Suite class in order to facilitate running integration tests from CLI. In
 * particular this has been added so as to be able to quickly test downstream
 * packaged thermostat.
 * </p>
 * <p>
 * After building thermostat from the same sources than packaged thermostat
 * locally via "mvn clean package", this produces the
 * thermostat-integration-tests-standalone-&lt;VERSION&gt;.jar in folder
 * integration-tests/standalone/target.
 * </p>
 * <p>
 * Concrete steps are:
 * 
 * <pre>
 *   $ wget http://icedtea.wildebeest.org/download/thermostat/thermostat-&lt;VERSION&gt.tar.gz
 *   $ tar -xzf thermostat-&lt;VERSION&gt.tar.gz
 *   $ cd thermostat-&lt;VERSION&gt
 *   $ mvn clean package 
 *   $ java -Dcom.redhat.thermostat.itest.thermostatHome=/path/to/thermostat/install \
 *            -Dcom.redhat.thermostat.itest.thermostatUserHome=$(echo ~/.thermostat) \
 *            -cp $(ls integration-tests/standalone/target/thermostat-integration-tests-standalone-*.jar)
 *            com.redhat.thermostat.itest.standalone.ItestRunner
 * </pre>
 * 
 * This should produce a human readable test report in
 * $(pwd)/thermostat-itest-reports/summary.txt
 * </p>
 * <p>
 * If you add more test classes, please add those classes to the set of
 * SuiteClasses.
 * </p>
 * <p>
 * Note that it's only useful to add expectj based tests to
 * the suite. MongoQueriesTest + WebAppTest don't seem to be suitable for
 * downstream packaged thermostat. They would require to be adjusted and/or
 * would need more jars to be on the class path. In particular packaged
 * storage-core, storage-mongodb etc. jars.
 * </p>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    CliTest.class,
    StorageConnectionTest.class,
    StorageTest.class,
    VmCommandsTest.class,
    PluginTest.class,
})
public class AllStandaloneTests {
    // nothing
}

