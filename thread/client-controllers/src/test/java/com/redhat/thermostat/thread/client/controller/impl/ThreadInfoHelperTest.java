/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.controller.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.thread.model.ThreadInfoData;

public class ThreadInfoHelperTest {

    @Test
    public void verifyMap() {
        ThreadInfoData data1 = new ThreadInfoData();
        data1.setThreadName("test1");
        data1.setThreadId(1);
        data1.setState(Thread.State.RUNNABLE);
        
        ThreadInfoData data2 = new ThreadInfoData();
        data2.setThreadName("test2");
        data2.setThreadId(2);
        data2.setState(Thread.State.BLOCKED);
        
        ThreadInfoData data3 = new ThreadInfoData();
        data3.setThreadName("test1");
        data3.setThreadId(1);
        data3.setState(Thread.State.TIMED_WAITING);
        
        ThreadInfoData data4 = new ThreadInfoData();
        data4.setThreadName("test2");
        data4.setThreadId(2);
        data4.setState(Thread.State.RUNNABLE);
        
        List<ThreadInfoData> infos = new ArrayList<>();
        infos.add(data1);
        infos.add(data2);
        infos.add(data3);
        infos.add(data4);
        
        Map<ThreadInfoData, List<ThreadInfoData>> result = ThreadInfoHelper.getThreadInfoDataMap(infos);
        assertEquals(2, result.size());
        
        assertTrue(result.containsKey(data1));
        assertTrue(result.containsKey(data2));
        
        assertEquals(2, result.get(data1).size());
        assertEquals(2, result.get(data2).size());
        
        assertEquals(Thread.State.RUNNABLE, result.get(data1).get(0).getState());
        assertEquals(Thread.State.TIMED_WAITING, result.get(data1).get(1).getState());       

        assertEquals(Thread.State.BLOCKED, result.get(data2).get(0).getState());
        assertEquals(Thread.State.RUNNABLE, result.get(data2).get(1).getState());
    }
}

