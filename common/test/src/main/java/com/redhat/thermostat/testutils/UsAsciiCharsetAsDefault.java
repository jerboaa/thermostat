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

package com.redhat.thermostat.testutils;

/**
 * Tests annotated with @Category(UsAsciiCharsetAsDefault.class) are expected to
 * be executed not only in the default charset of the testexecuting system. But
 * with the US-ASCII charset set as default charset. If you want to run this
 * tests in your ide pass -Dfile.encoding=US-ASCII as vm-argument. If you use
 * this Category for test that run with maven, make sure that the surefire
 * plugin settings match something like this.
 
 <pre>
      &lt;plugin&gt;
        &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
        &lt;artifactId&gt;maven-surefire-plugin&lt;/artifactId&gt;
        &lt;executions&gt;
          &lt;!-- run tests with UTF-8 encoding, use default-test goal so
               as to not run tests 3 times, 2 configs + default goal. --&gt;
          &lt;execution&gt;
            &lt;id&gt;default-test&lt;/id&gt;
            &lt;phase&gt;test&lt;/phase&gt;
            &lt;goals&gt;
              &lt;goal&gt;test&lt;/goal&gt;
            &lt;/goals&gt;
            &lt;configuration&gt;
              &lt;argLine&gt;${surefire-argline} ${coverageAgent} -Dfile.encoding=UTF-8&lt;/argLine&gt;
              &lt;forkMode&gt;always&lt;/forkMode&gt;
            &lt;/configuration&gt;
          &lt;/execution&gt;
          &lt;!-- run tests with US-ASCII encoding. See UsAsciiCharsetAsDefault --&gt;
          &lt;execution&gt;
            &lt;id&gt;tests-in-us-ascii&lt;/id&gt;
            &lt;phase&gt;test&lt;/phase&gt;
            &lt;goals&gt;
              &lt;goal&gt;test&lt;/goal&gt;
            &lt;/goals&gt;
            &lt;configuration&gt;
              &lt;forkMode&gt;always&lt;/forkMode&gt;
              &lt;argLine&gt;${surefire-argline} ${coverageAgent} -Dfile.encoding=US-ASCII&lt;/argLine&gt;
              &lt;groups&gt;com.redhat.thermostat.common.internal.test.UsAsciiCharsetAsDefault&lt;/groups&gt;
            &lt;/configuration&gt;
          &lt;/execution&gt;
        &lt;/executions&gt;
      &lt;/plugin&gt;
   </pre>
 */
public interface UsAsciiCharsetAsDefault {
    // empty
}
