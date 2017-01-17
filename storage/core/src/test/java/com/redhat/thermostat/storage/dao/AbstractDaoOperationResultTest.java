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

package com.redhat.thermostat.storage.dao;

import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AbstractDaoOperationResultTest {

    @Test
    public void testHasExceptions() {
        AbstractDaoOperationResult<VmInfo> abstractDaoOperationResult = getAbstractDaoOperationResult();
        assertFalse(abstractDaoOperationResult.hasExceptions());
        Exception e = new Exception();
        abstractDaoOperationResult.addException(e);
        assertTrue(abstractDaoOperationResult.hasExceptions());
    }

    @Test
    public void testAddException() {
        AbstractDaoOperationResult<VmInfo> abstractDaoOperationResult = getAbstractDaoOperationResult();
        Exception e1 = new Exception();
        abstractDaoOperationResult.addException(e1);
        assertFalse(abstractDaoOperationResult.getExceptions().isEmpty());
        assertThat(abstractDaoOperationResult.getExceptions(), is(equalTo(Collections.singletonList(e1))));
    }

    @Test
    public void testAddExceptions() {
        AbstractDaoOperationResult<VmInfo> abstractDaoOperationResult = getAbstractDaoOperationResult();
        Exception e1 = new Exception("foo");
        Exception e2 = new Exception("bar");
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(e1);
        exceptions.add(e2);
        abstractDaoOperationResult.addExceptions(exceptions);
        assertFalse(abstractDaoOperationResult.getExceptions().isEmpty());
        assertThat(abstractDaoOperationResult.getExceptions(), is(equalTo(exceptions)));
    }

    @Test
    public void testAddExceptionsIsDefensive() {
        // test that addExceptions makes a defensive copy, so that client code which bulk adds exceptions
        // is not able to add further exceptions to the AbstractDaoOperationResult by simply adding exceptions
        // to its own reference to the exceptions collection, rather than having to call through addException
        // or addExceptions again
        AbstractDaoOperationResult<VmInfo> abstractDaoOperationResult = getAbstractDaoOperationResult();
        Exception e1 = new Exception("foo");
        Exception e2 = new Exception("bar");
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(e1);
        exceptions.add(e2);
        abstractDaoOperationResult.addExceptions(exceptions);
        assertFalse(abstractDaoOperationResult.getExceptions().isEmpty());
        assertThat(abstractDaoOperationResult.getExceptions(), is(equalTo(exceptions)));
        Exception e3 = new Exception("baz");
        exceptions.add(e3);
        assertFalse(abstractDaoOperationResult.getExceptions().contains(e3));
    }

    @Test
    public void testGetExceptions() {
        AbstractDaoOperationResult<VmInfo> abstractDaoOperationResult = getAbstractDaoOperationResult();
        Exception e1 = new Exception("foo");
        Exception e2 = new Exception("bar");
        Collection<Exception> exceptions = new HashSet<>();
        exceptions.add(e1);
        exceptions.add(e2);
        abstractDaoOperationResult.addExceptions(exceptions);
        assertFalse(abstractDaoOperationResult.getExceptions().isEmpty());
        assertEquals(exceptions.size(), abstractDaoOperationResult.getExceptions().size());
        for (Exception e : exceptions) {
            assertTrue(abstractDaoOperationResult.getExceptions().contains(e));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetExceptionsImmutable() {
        getAbstractDaoOperationResult().getExceptions().add(new Exception());
    }

    private AbstractDaoOperationResult<VmInfo> getAbstractDaoOperationResult() {
        return new AbstractDaoOperationResult<VmInfo>() {
            @Override
            public Iterator<VmInfo> iterator() {
                VmInfo vmInfo1 = mock(VmInfo.class);
                VmInfo vmInfo2 = mock(VmInfo.class);
                return Arrays.asList(vmInfo1, vmInfo2).iterator();
            }
        };
    }

}
