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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;

public class SemanticsEnabledDescriptorParserTest {
    
    private static final String COMPLETE_SET_LIST_AGENT_INFO = "SET " +
            "'" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + AgentInfoDAO.START_TIME_KEY.getName() + "' = ?l , " +
            "'" + AgentInfoDAO.STOP_TIME_KEY.getName() + "' = ?l , " +
            "'" + AgentInfoDAO.ALIVE_KEY.getName() + "' = ?b , " +
            "'" + AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName() + "' = ?s";
    
    private static final String INCOMPLETE_SET_LIST_AGENT_INFO = "SET " +
            "'" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + AgentInfoDAO.START_TIME_KEY.getName() + "' = ?l , " +
            // stop-time key missing
            "'" + AgentInfoDAO.ALIVE_KEY.getName() + "' = ?b , " +
            "'" + AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName() + "' = ?s";

    private BackingStorage storage;
    private Query<AgentInformation> mockQuery;
    private AggregateQuery<AgentInformation> aggQuery;
    private SemanticsEnabledDescriptorParser<AgentInformation> parser;
    private Add<AgentInformation> mockAdd;
    private Update<AgentInformation> mockUpdate;
    private Replace<AgentInformation> mockReplace;
    private Remove<AgentInformation> mockRemove;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        storage = mock(BackingStorage.class);
        mockQuery = mock(Query.class);
        aggQuery = mock(AggregateQuery.class);
        // setup for QUERY/QUERY-COUNT/QUERY-DISTINCT
        when(storage.createQuery(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockQuery);
        when(storage.createAggregateQuery(eq(AggregateFunction.COUNT), (eq(AgentInfoDAO.CATEGORY)))).thenReturn(aggQuery);
        when(storage.createAggregateQuery(eq(AggregateFunction.DISTINCT), (eq(AgentInfoDAO.CATEGORY)))).thenReturn(aggQuery);
        // setup for ADD
        mockAdd = mock(Add.class);
        when(storage.createAdd(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockAdd);
        // setup for UPDATE
        mockUpdate = mock(Update.class);
        when(storage.createUpdate(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockUpdate);
        // setup for REMOVE
        mockRemove = mock(Remove.class);
        when(storage.createRemove(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockRemove);
        // setup for REPLACE
        mockReplace = mock(Replace.class);
        when(storage.createReplace(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockReplace);
    }
    
    @Test
    public void canParseQueryWithLimit() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " LIMIT 1";
        doSemanticsBasicParseTest(descString);
        
    }
    
    @Test
    public void catParseQueryCountWithLimit() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.COUNT);
        String descString = "QUERY-COUNT " + AgentInfoDAO.CATEGORY.getName() + " LIMIT 1";
        doSemanticsBasicParseTest(descString);
    }
    
    @Test
    public void canParseQueryCountWithCorrectKeyParam() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.COUNT);
        String descString = "QUERY-COUNT(" + Key.AGENT_ID.getName() + ") "
                              + AgentInfoDAO.CATEGORY.getName();
        doSemanticsBasicParseTest(descString);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void canParseQueryDistinctWithCorrectKeyParam() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.DISTINCT);
        when(aggQuery.getAggregateKey()).thenReturn((Key)Key.AGENT_ID);
        String descString = "QUERY-DISTINCT(" + Key.AGENT_ID.getName() + ") "
                              + AgentInfoDAO.CATEGORY.getName();
        doSemanticsBasicParseTest(descString);
    }
    
    @Test
    public void canParseQueryWithSort() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " SORT 'foo' DSC";
        doSemanticsBasicParseTest(descString);
    }
    
    @Test
    public void catParseQueryCountWithSort() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.COUNT);
        String descString = "QUERY-COUNT " + AgentInfoDAO.CATEGORY.getName() + " SORT 'foo' DSC";
        doSemanticsBasicParseTest(descString);
    }
    
    /*
     * Tests whether parse succeeds if some properties are missing from the
     * UPDATE descriptor. Update is the operation to be used in this case.
     */
    @Test
    public void canParseUpdateWithSomePropertiesMissing() throws DescriptorParsingException {
        String descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() +
                    " SET '" + Key.AGENT_ID.getName() + "' = 'b' , " + 
                         "'"+ AgentInfoDAO.ALIVE_KEY.getName() + "' = ?b" +
                    " WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
        doSemanticsBasicParseTest(descString);
    }
    
    /*
     * Tests whether parse succeeds if ALL properties are given in the
     * ADD descriptor.
     */
    @Test
    public void canParseAddWithAllPropertiesGiven() throws DescriptorParsingException {
        String descString = "ADD " + AgentInfoDAO.CATEGORY.getName() + " " + COMPLETE_SET_LIST_AGENT_INFO;
        doSemanticsBasicParseTest(descString);
    }
    
    /*
     * Tests whether parse succeeds if ALL properties are given in the
     * REPLACE descriptor.
     */
    @Test
    public void canParseReplaceWithAllPropertiesGiven() throws DescriptorParsingException {
        String descString = "REPLACE " + AgentInfoDAO.CATEGORY.getName() +
                " " + COMPLETE_SET_LIST_AGENT_INFO +
                " WHERE '" + AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName()+ "' = ?s";
        doSemanticsBasicParseTest(descString);
    }
    
    private void doSemanticsBasicParseTest(String strDesc) throws DescriptorParsingException {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        ParsedStatement<AgentInformation> p = parser.parse();
        assertNotNull(p);
    }
    
    /*
     * Tests whether we appropriately reject a provided key parameter in a
     * query-count aggregate for which the key is unknown in the given category.
     */
    @Test
    public void rejectQueryCountWithUnknownKeyAsParam() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.COUNT);
        doRejectQueryAggregateTestWithParam(AggregateFunction.COUNT);
    }
    
    /*
     * Tests whether we appropriately reject a provided key parameter in a
     * query-distinct aggregate for which the key is unknown in the given category.
     */
    @Test
    public void rejectQueryDistinctWithUnknownKeyAsParam() throws DescriptorParsingException {
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.DISTINCT);
        doRejectQueryAggregateTestWithParam(AggregateFunction.DISTINCT);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doRejectQueryAggregateTestWithParam(AggregateFunction function) {
        String unknownKeyName = "unknown_key";
        Key unknownKey = new Key(unknownKeyName);
        when(aggQuery.getAggregateKey()).thenReturn(unknownKey);
        String format = "QUERY-%s(%s) %s";
        String descString = String.format(format, function.name(), unknownKeyName, AgentInfoDAO.CATEGORY.getName());
        assertNull("Precondition failed. unknown_key in AgentInfo category?",
                AgentInfoDAO.CATEGORY.getKey(unknownKeyName));
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail(String.format("QUERY-%s with an unknown key as param should not parse", function.name()));
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Unknown aggregate key '" + unknownKeyName + "'"));
        }
    }
    
    /*
     * Distinct queries aggregate by selecting all distinct values for a
     * given key. Not providing a key for which to find distinct values for 
     * doesn't make sense.
     */
    @Test
    public void rejectQueryDistinctWithoutKeyParam() throws DescriptorParsingException {
        String descString = "QUERY-DISTINCT " + AgentInfoDAO.CATEGORY.getName();
        
        when(aggQuery.getAggregateKey()).thenReturn(null);
        when(aggQuery.getAggregateFunction()).thenReturn(AggregateFunction.DISTINCT);
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("QUERY-DISTINCT without a key for which to produce distinct values for should not parse");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Aggregate key for DISTINCT must not be null"));
        }
    }
    
    @Test
    public void rejectAddWithWhere() throws DescriptorParsingException {
        String descString = "ADD " + AgentInfoDAO.CATEGORY.getName() + " " + COMPLETE_SET_LIST_AGENT_INFO + " WHERE 'a' = 'b'";
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("ADD operation does not support WHERE clauses!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("WHERE clause not allowed for ADD"));
        }
    }
    
    @Test
    public void rejectReplaceWithoutWhere() throws DescriptorParsingException {
        String descString = "REPLACE " + AgentInfoDAO.CATEGORY.getName() + " " + COMPLETE_SET_LIST_AGENT_INFO;
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("REPLACE operation requires WHERE clause, and should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("WHERE clause required for REPLACE"));
        }
    }
    
    @Test
    public void rejectUpdateWithoutWhere() throws DescriptorParsingException {
        String descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() +
                " SET '" + Key.AGENT_ID.getName() + "' = 'b' , 'c' = 'd'";
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("UPDATE operation requires WHERE clause, and should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("WHERE clause required for UPDATE"));
        }
    }
    
    @Test
    public void rejectRemoveWithSetList() throws DescriptorParsingException {
        String descString = "REMOVE " + AgentInfoDAO.CATEGORY.getName() + " SET 'a' = 'b'" +
                                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'";
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("REMOVE does not allow SET list, and should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue("message was: " + e.getMessage(), e.getMessage().contains("SET not allowed for REMOVE"));
        }
    }
    
    @Test
    public void rejectQueryWithSetList() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " SET 'a' = 'b'" +
                                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'";
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("QUERY does not allow SET list, and should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("SET not allowed for QUERY"));
        }
    }
    
    
    private void doRejectWriteSortLimitTest(String descString, String failmsg) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail(failmsg);
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("LIMIT/SORT only allowed for QUERY/QUERY-COUNT", e.getMessage());
        }
    }
    
    @Test
    public void rejectWritesWithSortLimit() throws DescriptorParsingException {
        // Update rejects
        String descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() +
                " SET '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " SORT 'a' DSC"; // this is intentionally wrong for this test
        String failmsg = "SORT in UPDATE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() +
                " SET '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " LIMIT 1"; // this is intentionally wrong for this test
        failmsg = "LIMIT in UPDATE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        
        // Remove rejects
        descString = "REMOVE " + AgentInfoDAO.CATEGORY.getName() +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " LIMIT 1"; // this is intentionally wrong for this test
        failmsg = "LIMIT in REMOVE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        descString = "REMOVE " + AgentInfoDAO.CATEGORY.getName() +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " SORT 'a' ASC";
        failmsg = "SORT in REMOVE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        
        // Replace rejects
        descString = "REPLACE " + AgentInfoDAO.CATEGORY.getName() + " " +
                COMPLETE_SET_LIST_AGENT_INFO +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " LIMIT 1";
        failmsg = "LIMIT in REPLACE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        descString = "REPLACE " + AgentInfoDAO.CATEGORY.getName() + " " +
                COMPLETE_SET_LIST_AGENT_INFO +
                " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'" +
                " SORT 'a' ASC";
        failmsg = "SORT in REPLACE is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        
        // Add rejects
        descString = "ADD " + AgentInfoDAO.CATEGORY.getName() + " " + COMPLETE_SET_LIST_AGENT_INFO + " LIMIT 1";
        failmsg = "LIMIT in ADD is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
        descString = "ADD " + AgentInfoDAO.CATEGORY.getName() + " " + COMPLETE_SET_LIST_AGENT_INFO + " SORT 'a' ASC";
        failmsg = "SORT in ADD is not allowed, and should not parse!";
        doRejectWriteSortLimitTest(descString, failmsg);
    }
    
    @Test
    public void rejectUpdateWithoutSet() throws DescriptorParsingException {
        String descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() + " WHERE '" + Key.AGENT_ID.getName() + "' = 'b'";
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("UPDATE requires SET list, and should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("SET list required for UPDATE", e.getMessage());
        }
    }
    
    /*
     * Tests whether parser fails if some properties are missing from the
     * ADD descriptor, but the category specified as keys.
     */
    @Test
    public void rejectAddWithSomePropertiesMissing() throws DescriptorParsingException {
        String strDesc = "ADD " + AgentInfoDAO.CATEGORY.getName() + " " + INCOMPLETE_SET_LIST_AGENT_INFO;
        doRejectIncompleteKeysTest(strDesc);
    }
    
    /*
     * Tests whether parser fails if some properties are missing from the
     * REPLACE descriptor, but the category specified as keys.
     */
    @Test
    public void rejectReplaceWithSomePropertiesMissing() throws DescriptorParsingException {
        // The following descriptor should be valid except for the one missing key.
        String strDesc = "REPLACE " + AgentInfoDAO.CATEGORY.getName() + " "
                        + INCOMPLETE_SET_LIST_AGENT_INFO + " WHERE 'foo' = ?s";
        doRejectIncompleteKeysTest(strDesc);
    }

    private void doRejectIncompleteKeysTest(String strDesc) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("stop timestamp key missing. Should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Keys don't match keys in category."));
        }
    }
    
    /*
     * Tests whether parser fails if LHS of any SET pair of ADD/REPLACE is a
     * free variable. We disallow this, since key completeness checks don't make
     * much sense (prior patching) without this.
     */
    @Test
    public void rejectAddReplaceWithFreeVarOnLHS() throws DescriptorParsingException {
        // second pair has ?s as LHS 
        String strDesc = "ADD " + AgentInfoDAO.CATEGORY.getName() + " SET " + 
                "'" + Key.AGENT_ID.getName() + "' = ?s , ?s = ?b";
        doRejectWithFreeVarOnLHS(strDesc);
        
        // ?b is the offending part
        strDesc = "REPLACE " + AgentInfoDAO.CATEGORY.getName() + " SET " + 
                "?b = ?s WHERE 'foo' = ?s";
        doRejectWithFreeVarOnLHS(strDesc);
    }

    private void doRejectWithFreeVarOnLHS(String strDesc) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("SET with free variable on LHS should not parse!");
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("LHS of set list pair must not be a free variable.", e.getMessage());
        }
    }
    
    /*
     * Tests whether a key set via UPDATE exists in the category. 
     */
    @Test
    public void rejectUpdateWithUnknownKeyInSet() {
        String descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() + " SET 'iDoNotExist' = ?s WHERE 'foo' = 'b'";
        String detail = "'[iDoNotExist]'";
        doRejectUpdateWithUnknownKeyTest(descString, detail);
        descString = "UPDATE " + AgentInfoDAO.CATEGORY.getName() + " SET 'iDoNotExist' = ?s , 'notHere' = ?b WHERE 'foo' = 'b'";
        detail = "'[iDoNotExist, notHere]'";
        doRejectUpdateWithUnknownKeyTest(descString, detail);
    }

    private void doRejectUpdateWithUnknownKeyTest(String descString,
            String detailMsg) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, descString);
        parser = new SemanticsEnabledDescriptorParser<>(storage, desc);
        try {
            parser.parse();
            fail("SET for unknown key in category should not parse");
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("Unknown key(s) in SET: " + detailMsg, e.getMessage());
        }
    }
    
}

