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

package com.redhat.thermostat.host.memory.agent.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.utils.ProcDataSource;

public class HostMemoryBackend extends BaseBackend {

    private static final long PROC_CHECK_INTERVAL = 1000; // TODO make this configurable.
    
    private final MemoryStatBuilder memoryStatBuilder;
    private MemoryStatDAO memoryStats;
    private ScheduledExecutorService executor;
    private boolean started;

    public HostMemoryBackend(ScheduledExecutorService executor, MemoryStatDAO memoryStatDAO, Version version, final WriterID writerId) {
        super("Host Memory Backend",
                "Gathers memory statistics about a host",
                "Red Hat, Inc.",
                version.getVersionNumber(), true);
        this.executor = executor;
        this.memoryStats = memoryStatDAO;

        ProcDataSource source = new ProcDataSource();
        memoryStatBuilder = new MemoryStatBuilder(source, writerId);
    }

    @Override
    public boolean activate() {
        if (!started) {
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    memoryStats.putMemoryStat(memoryStatBuilder.build());
                }
            }, 0, PROC_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
            started = true;
        }
        return true;
    }

    @Override
    public boolean deactivate() {
        executor.shutdown();
        started = false;
        return true;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP;
    }

}

