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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Objects;

public class HeapCommandHelper {

    public static final int MAXIMUM_CACHED_HELPERS = 5;

    static final String HEAP_ID_ARG = "heapId";
    static final String OBJECT_ID_ARG = "objectId";

    protected static final Object getLock = new Object();
    protected static final Map<HeapDumpIdentifier, SoftReference<HeapCommandHelper>> helperCache = new LRUMap<>(MAXIMUM_CACHED_HELPERS);

    protected HeapDAO dao;
    protected String heapId;
    protected HeapDump heapDump;

    // package private for testing only
    HeapCommandHelper(HeapDumpIdentifier identifier) {
        this.heapId = identifier.heapId;
        this.dao = identifier.dao;
    }

    public static HeapCommandHelper getHelper(CommandContext ctx, HeapDAO heapDAO) {
        synchronized (getLock) {
            HeapDumpIdentifier heapDumpIdentifier = HeapDumpIdentifier.makeIdentifier(ctx, heapDAO);
            HeapCommandHelper helper;
            if (!helperCache.containsKey(heapDumpIdentifier)) {
                helper = new HeapCommandHelper(heapDumpIdentifier);
                helperCache.put(heapDumpIdentifier, new SoftReference<>(helper));
            } else {
                HeapCommandHelper _h = helperCache.get(heapDumpIdentifier).get();
                if (_h == null) {
                    helper = new HeapCommandHelper(heapDumpIdentifier);
                    helperCache.put(heapDumpIdentifier, new SoftReference<>(helper));
                } else {
                    helper = _h;
                }
            }
            return helper;
        }
    }

    protected void loadHeapDump() throws HeapNotFoundException {
        HeapInfo heapInfo = dao.getHeapInfo(heapId);
        if (heapInfo == null) {
            throw new HeapNotFoundException(heapId);
        }
        HeapDump heapDump = dao.getHeapDump(heapInfo);
        if (heapDump == null) {
            throw new HeapNotFoundException(heapId);
        }
        this.heapDump = heapDump;
    }

    public HeapDump getHeapDump() throws HeapNotFoundException {
        if (heapDump == null) {
            loadHeapDump();
        }
        return heapDump;
    }

    public JavaHeapObject getJavaHeapObject(CommandContext ctx) throws CommandException {
        Arguments arguments = ctx.getArguments();
        if (!arguments.hasArgument(OBJECT_ID_ARG)) {
            throw new ObjectIdRequiredException();
        }
        HeapDump heapDump = getHeapDump();
        String objectId = arguments.getArgument(OBJECT_ID_ARG);
        JavaHeapObject obj = heapDump.findObject(objectId);
        if (obj == null) {
            throw new ObjectNotFoundException(objectId);
        }
        return obj;
    }

    protected static class HeapDumpIdentifier {

        protected final String heapId;
        protected final HeapDAO dao;

        // package private for testing only
        HeapDumpIdentifier(String heapId, HeapDAO dao) {
            this.heapId = Objects.requireNonNull(heapId);
            this.dao = Objects.requireNonNull(dao);
        }

        public static HeapDumpIdentifier makeIdentifier(CommandContext ctx, HeapDAO dao) {
            String heapId = ctx.getArguments().getArgument(HEAP_ID_ARG);
            return new HeapDumpIdentifier(heapId, dao);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HeapDumpIdentifier heapDumpIdentifier = (HeapDumpIdentifier) o;

            return heapId.equals(heapDumpIdentifier.heapId) && dao.equals(heapDumpIdentifier.dao);
        }

        @Override
        public int hashCode() {
            int result = heapId.hashCode();
            result = 31 * result + dao.hashCode();
            return result;
        }
    }

}
