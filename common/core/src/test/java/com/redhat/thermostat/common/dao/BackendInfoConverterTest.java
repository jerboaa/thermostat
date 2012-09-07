/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.model.BackendInformation;
import com.redhat.thermostat.common.storage.Chunk;

public class BackendInfoConverterTest {

    @Test
    public void testFromChunk() {
        final String BACKEND_NAME = "test-backend";
        final String BACKEND_DESC = "test-backend-description-that-may-be-long";
        final Boolean ACTIVE = true;
        final List<Integer> TO_MONITOR = Arrays.asList(new Integer[] { -1, 0, 1 });
        final Boolean MONITOR_NEW = false;

        Chunk input = new Chunk(BackendInfoDAO.CATEGORY, true);
        input.put(BackendInfoDAO.BACKEND_NAME, BACKEND_NAME);
        input.put(BackendInfoDAO.BACKEND_DESCRIPTION, BACKEND_DESC);
        input.put(BackendInfoDAO.IS_ACTIVE, ACTIVE);
        input.put(BackendInfoDAO.PIDS_TO_MONITOR, TO_MONITOR);
        input.put(BackendInfoDAO.SHOULD_MONITOR_NEW_PROCESSES, MONITOR_NEW);

        BackendInfoConverter converter = new BackendInfoConverter();

        BackendInformation result = converter.fromChunk(input);

        assertEquals(BACKEND_NAME, result.getName());
        assertEquals(BACKEND_DESC, result.getDescription());
        assertEquals(ACTIVE, result.isActive());
        assertEquals(MONITOR_NEW, result.isObserveNewJvm());
        assertEquals(TO_MONITOR, result.getPids());
    }

    @Test
    public void testToChunk() {
        final String BACKEND_NAME = "test-backend";
        final String BACKEND_DESC = "test-backend-description-that-may-be-long";
        final Boolean ACTIVE = true;
        final List<Integer> TO_MONITOR = Arrays.asList(new Integer[] { -1, 0, 1 });
        final Boolean MONITOR_NEW = false;

        BackendInformation backendInfo = new BackendInformation();
        backendInfo.setName(BACKEND_NAME);
        backendInfo.setDescription(BACKEND_DESC);
        backendInfo.setActive(ACTIVE);
        backendInfo.setObserveNewJvm(MONITOR_NEW);
        backendInfo.setPids(TO_MONITOR);

        BackendInfoConverter converter = new BackendInfoConverter();

        Chunk result = converter.toChunk(backendInfo);

        assertEquals(BackendInfoDAO.CATEGORY, result.getCategory());

        assertEquals(BACKEND_NAME, result.get(BackendInfoDAO.BACKEND_NAME));
        assertEquals(BACKEND_DESC, result.get(BackendInfoDAO.BACKEND_DESCRIPTION));
        assertEquals(ACTIVE, result.get(BackendInfoDAO.IS_ACTIVE));
        assertEquals(TO_MONITOR, result.get(BackendInfoDAO.PIDS_TO_MONITOR));
        assertEquals(MONITOR_NEW, result.get(BackendInfoDAO.SHOULD_MONITOR_NEW_PROCESSES));

    }

}
