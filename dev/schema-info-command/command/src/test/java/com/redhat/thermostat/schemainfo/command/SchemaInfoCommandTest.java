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

package com.redhat.thermostat.schemainfo.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.SchemaInfoDAO;
import com.redhat.thermostat.storage.model.SchemaInformation;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.schemainfo.command.internal.SchemaInfoCommand;
import com.redhat.thermostat.schemainfo.command.locale.LocaleResources;

public class SchemaInfoCommandTest {
    
    private static Translate<LocaleResources> translator;
    private SchemaInfoCommand cmd;
    private CommandContext ctxt;
    private Console console;
    private List<SchemaInformation> list;
    private PrintStream output;
    private PrintStream error;
    private SchemaInfoDAO dao;
    private SchemaInformation categoryTest1;
    private SchemaInformation categoryTest2;
    private StubBundleContext context;
        
    @Before
    public void setUp() {
        translator = LocaleResources.createLocalizer();
        categoryTest1 = new SchemaInformation();
        categoryTest2 = new SchemaInformation();
        ctxt = mock(CommandContext.class);
        console = mock(Console.class);
        list = new ArrayList<>();
        
        categoryTest1.setCategoryName("category Test 1");
        categoryTest1.setTimeStamp(System.currentTimeMillis());
        categoryTest2.setCategoryName("category Test 2");
        categoryTest2.setTimeStamp(System.currentTimeMillis());
        list.add(categoryTest1);
        list.add(categoryTest2);
        
        dao = mock(SchemaInfoDAO.class);
        context = new StubBundleContext();
        context.registerService(SchemaInfoDAO.class, dao, null);
        cmd = new SchemaInfoCommand(context);
        
        when(ctxt.getConsole()).thenReturn(console);
        when(console.getError()).thenReturn(error);
        when(console.getOutput()).thenReturn(output);
        
    }
    
    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }
    
    @Test
    public void verifyGetCategories() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        when(dao.getSchemaInfos()).thenReturn(list);
        
        cmd.run(factory.createContext(args));
        
        String expected = categoryTest1.getCategoryName() + "\n" +
                         categoryTest2.getCategoryName() + "\n";

        assertEquals(expected, factory.getOutput());
    }
    
    @Test
    public void verifyGetNoCategories() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        List<SchemaInformation> emptylist = Collections.emptyList();
        
        when(dao.getSchemaInfos()).thenReturn(emptylist);
        cmd.run(factory.createContext(args));
        
        String expected = translator.localize(LocaleResources.NO_CATEGORIES).getContents() + "\n";
        assertEquals(expected, factory.getOutput());
    }
    
}

