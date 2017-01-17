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

package com.redhat.thermostat.storage.testutils;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;

public class StatementDescriptorTesterTest {

    private static final Key<String> barKey = new Key<>("bar");
    private static final Category<TestPojo> testCategory = new Category<>("desc-tester-category", TestPojo.class, barKey);
    
    @Test(expected = DescriptorParsingException.class)
    public void canDetermineBadDescriptorBasic() throws DescriptorParsingException {
        String brokenDesc = "foo";
        StatementDescriptor<TestPojo> desc = new StatementDescriptor<>(testCategory, brokenDesc);
        StatementDescriptorTester<TestPojo> tester = new StatementDescriptorTester<>();
        tester.testParseBasic(desc); // should throw exception
    }
    
    public void canDetermineGoodDescriptorBasic() throws DescriptorParsingException {
        // This desc has correct syntax, but is semantically incorrect. The
        // basic parser does not check this though.
        String goodDesc = "QUERY desc-tester-category WHERE 'foo' = ?l";
        StatementDescriptor<TestPojo> desc = new StatementDescriptor<>(testCategory, goodDesc);
        StatementDescriptorTester<TestPojo> tester = new StatementDescriptorTester<>();
        tester.testParseBasic(desc); // must not throw exception
    }
    
    @Test(expected = DescriptorParsingException.class)
    public void canDetermineBadDescriptorSemantic() throws DescriptorParsingException {
        // No such key 'foo' in category. Thus, DPE on parse()
        String brokenDesc = "ADD desc-tester-category SET 'foo' = ?l";
        StatementDescriptor<TestPojo> desc = new StatementDescriptor<>(testCategory, brokenDesc);
        StatementDescriptorTester<TestPojo> tester = new StatementDescriptorTester<>();
        tester.testParseSemantic(desc); // should throw exception
    }
    
    @Test
    public void canDetermineGoodDescriptorSemantic() throws DescriptorParsingException {
        String goodDesc = "ADD desc-tester-category SET 'bar' = ?l";
        StatementDescriptor<TestPojo> desc = new StatementDescriptor<>(testCategory, goodDesc);
        StatementDescriptorTester<TestPojo> tester = new StatementDescriptorTester<>();
        tester.testParseSemantic(desc); // must not throw exception
    }
    
    static class TestPojo implements Pojo {
        // empty
    }
}
