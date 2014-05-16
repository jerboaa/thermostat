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

package com.redhat.thermostat.thread.harvester;

import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHeader;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ThreadInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/*
 */
public class ThreadHeaderHelperTest {

    private static String DEFAULT_W_ID = "0xcafe";
    private static String DEFAULT_THREAD_NAME = "Thread42";
    private static long DEFAULT_THREAD_ID = 42;

    private String vmId;
    private WriterID writerId;
    private ThreadDao threadDao;

    private ThreadInfo info;

    @Before
    public void setUp() throws Exception {
        vmId = "testVM";
        writerId  = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(DEFAULT_W_ID);
        
        threadDao = mock(ThreadDao.class);

        info = mock(ThreadInfo.class);
        when(info.getThreadName()).thenReturn(DEFAULT_THREAD_NAME);
        when(info.getThreadId()).thenReturn(DEFAULT_THREAD_ID);
    }

    @Test
    public void testCreateThreadHeader() throws Exception {

        ThreadHeaderHelper helper = new ThreadHeaderHelper(threadDao, writerId, vmId);
        long timestamp = -1l;

        ThreadHeader header = helper.createThreadHeader(info, timestamp);
        assertNotNull(header);

        verify(writerId).getWriterID();

        assertEquals(header.getVmId(), vmId);
        assertEquals(header.getAgentId(), DEFAULT_W_ID);
        assertEquals(header.getTimeStamp(), timestamp);
        assertEquals(header.getThreadName(), DEFAULT_THREAD_NAME);
        assertEquals(header.getThreadId(), DEFAULT_THREAD_ID);
    }

    @Test
    public void testSaveThreadHeader() throws Exception {
        ThreadHeaderHelper helper = new ThreadHeaderHelper(threadDao, writerId, vmId);

        ThreadHeader template = new ThreadHeader(DEFAULT_W_ID);
        template.setReferenceID("1234");
        template.setVmId(vmId);
        template.setThreadName(DEFAULT_THREAD_NAME);
        template.setThreadId(DEFAULT_THREAD_ID);
        template.setTimeStamp(-1l);

        // first time around, object not present in database
        when(threadDao.getThread(template)).thenReturn(null);

        ThreadHeader result = helper.checkAndSaveThreadHeader(template);
        assertNotNull(result);

        verify(threadDao).getThread(template);
        verify(threadDao).saveThread(template);

        assertEquals(result.getVmId(), template.getVmId());
        assertEquals(result.getAgentId(), template.getAgentId());
        assertEquals(result.getTimeStamp(), template.getTimeStamp());
        assertEquals(result.getThreadName(), template.getThreadName());
        assertEquals(result.getThreadId(), template.getThreadId());
        assertEquals(result, template);
    }

    @Test
    public void testSaveThreadHeader2() throws Exception {
        ThreadHeaderHelper helper = new ThreadHeaderHelper(threadDao, writerId, vmId);

        ThreadHeader template = new ThreadHeader(DEFAULT_W_ID);
        template.setReferenceID("1234");
        template.setVmId(vmId);
        template.setThreadName(DEFAULT_THREAD_NAME);
        template.setThreadId(DEFAULT_THREAD_ID);
        template.setTimeStamp(-1l);

        ThreadHeader expected = new ThreadHeader(DEFAULT_W_ID);
        expected.setReferenceID("0000");
        expected.setVmId(vmId);
        expected.setThreadName(DEFAULT_THREAD_NAME);
        expected.setThreadId(DEFAULT_THREAD_ID);
        expected.setTimeStamp(-2l);

        // second time around, object is present in database, we need
        // to check that the two headers are actually the same, but the
        // returned value has the correct refId and timestamp
        when(threadDao.getThread(template)).thenReturn(expected);

        ThreadHeader result = helper.checkAndSaveThreadHeader(template);
        assertNotNull(result);

        verify(threadDao).getThread(template);
        verify(threadDao, times(0)).saveThread(template);

        assertEquals(result.getVmId(), expected.getVmId());
        assertEquals(result.getAgentId(), expected.getAgentId());
        assertEquals(result.getTimeStamp(), expected.getTimeStamp());
        assertEquals(result.getThreadName(), expected.getThreadName());
        assertEquals(result.getThreadId(), expected.getThreadId());
        assertEquals(result, expected);
    }
}
