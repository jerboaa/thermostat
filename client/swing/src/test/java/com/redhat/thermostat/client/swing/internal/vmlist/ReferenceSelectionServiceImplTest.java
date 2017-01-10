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

package com.redhat.thermostat.client.swing.internal.vmlist;

import com.redhat.thermostat.client.swing.ReferenceSelectionChangeListener;
import com.redhat.thermostat.client.swing.ReferenceSelectionChangedEvent;
import com.redhat.thermostat.client.swing.ReferenceSelectionService;
import com.redhat.thermostat.storage.core.Ref;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 */
public class ReferenceSelectionServiceImplTest {

    private static final int SELECTED = 1;
    private static final int OLD = 0;

    @Test
    public void testSetReferenceFiresEvent() throws Exception {

        Ref oldRef = Mockito.mock(Ref.class);
        Ref newRef = Mockito.mock(Ref.class);

        final Ref[] references = new Ref[2];

        ReferenceSelectionServiceImpl service = new ReferenceSelectionServiceImpl();
        service.addReferenceSelectionChangeListener(new ReferenceSelectionChangeListener() {
            @Override
            public void referenceChanged(ReferenceSelectionChangedEvent event) {
                references[OLD] = event.getOld();
                references[SELECTED] = event.getSelected();
            }
        });

        service.setReference(oldRef);
        assertNull(references[OLD]);
        assertEquals(oldRef, references[SELECTED]);

        service.setReference(newRef);
        assertEquals(oldRef, references[OLD]);
        assertEquals(newRef, references[SELECTED]);
    }

    @Test
    public void testRemoveReferenceSelectionChangeListener() throws Exception {

        Ref newRef = Mockito.mock(Ref.class);

        final boolean[] result = new boolean[1];
        result[0] = true;

        ReferenceSelectionServiceImpl service = new ReferenceSelectionServiceImpl();
        ReferenceSelectionChangeListener listener = new ReferenceSelectionChangeListener() {
            @Override
            public void referenceChanged(ReferenceSelectionChangedEvent event) {
                result[0] = false;
            }
        };

        service.addReferenceSelectionChangeListener(listener);
        service.removeReferenceSelectionChangeListener(listener);

        service.setReference(newRef);
        assertTrue(result[0]);
    }
}