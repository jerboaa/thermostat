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

package com.redhat.thermostat.vm.heap.analysis.common.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.CloseOnSave;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

import static com.redhat.thermostat.common.utils.IteratorUtils.asList;
import static com.redhat.thermostat.common.utils.IteratorUtils.head;

public class HeapDAOImpl implements HeapDAO {

    private static final Logger log = LoggingUtils.getLogger(HeapDAOImpl.class);
    
    // Query descriptors
    
    static final String QUERY_ALL_HEAPS = "QUERY "
            + heapInfoCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s";
    static final String QUERY_HEAP_INFO = "QUERY "
            + heapInfoCategory.getName() + " WHERE '" 
            + heapIdKey.getName() + "' = ?s LIMIT 1";

    // Write descriptors
    
    // ADD vm-heap-info SET 'agentId' = ?s , \
    //                      'vmId' = ?s , \
    //                      'timeStamp' = ?l , \
    //                      'heapId' = ?s , \
    //                      'heapDumpId' = ?s , \
    //                      'histogramId' = ?s
    static final String DESC_ADD_VM_HEAP_INFO = "ADD " + heapInfoCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + heapIdKey.getName() + "' = ?s , " +
                 "'" + heapDumpIdKey.getName() + "' = ?s , " +
                 "'" + histogramIdKey.getName() + "' = ?s";
    
    private final Storage storage;

    HeapDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(heapInfoCategory);
    }

    @Override
    public void putHeapInfo(HeapInfo heapInfo, final File heapDumpData, ObjectHistogram histogramData, Runnable heapDumpCleanup) throws IOException {
        String heapId = heapInfo.getAgentId() + "-" + heapInfo.getVmId() + "-" + heapInfo.getTimeStamp();
        System.err.println("assigning heapId: " + heapId);
        heapInfo.setHeapId(heapId);
        String heapDumpId = "heapdump-" + heapId;
        String histogramId = "histogram-" + heapId;
        if (heapDumpData != null) {
            heapInfo.setHeapDumpId(heapDumpId);
        }
        if (histogramData != null) {
            heapInfo.setHistogramId(histogramId);
        }
        addHeapInfo(heapInfo);

        if (heapDumpData != null) {
            uploadHeapDump(heapDumpData, heapDumpId, heapDumpCleanup);
        }
        if (histogramData != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(histogramData);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                storage.saveFile(histogramId, bais, new CloseOnSave(bais));
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Level.SEVERE, "Unexpected error while writing histogram", e);
            }
        }
    }

    private void addHeapInfo(HeapInfo heapInfo) {
        StatementDescriptor<HeapInfo> desc = new StatementDescriptor<>(heapInfoCategory, DESC_ADD_VM_HEAP_INFO);
        PreparedStatement<HeapInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, heapInfo.getAgentId());
            prepared.setString(1, heapInfo.getVmId());
            prepared.setLong(2, heapInfo.getTimeStamp());
            prepared.setString(3, heapInfo.getHeapId());
            prepared.setString(4, heapInfo.getHeapDumpId());
            prepared.setString(5, heapInfo.getHistogramId());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            log.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            log.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    private void uploadHeapDump(final File heapDumpData, String heapDumpId, final Runnable heapDumpCleanup)
            throws FileNotFoundException {
        final InputStream heapDumpStream = new FileInputStream(heapDumpData);
        storage.saveFile(heapDumpId, heapDumpStream, new SaveFileListener() {

            @Override
            public void notify(EventType type, Object additionalArguments) {
                try {
                    switch (type) {
                    case EXCEPTION_OCCURRED:
                        StorageException cause = (StorageException) additionalArguments;
                        log.log(Level.SEVERE, "Error saving heap dump", cause);
                        break;
                    case SAVE_COMPLETE:
                        break;
                    default:
                        log.log(Level.WARNING, "Unknown saveFile event: " + type);
                    }
                    heapDumpStream.close();
                    heapDumpCleanup.run();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Exception when saving file", e);
                }

            }
        });
    }

    @Override
    public Collection<HeapInfo> getAllHeapInfo(VmRef vm) {
        return getAllHeapInfo(new AgentId(vm.getHostRef().getAgentId()), new VmId(vm.getVmId()));
    }

    @Override
    public Collection<HeapInfo> getAllHeapInfo(AgentId agentId, VmId vmId) {
        StatementDescriptor<HeapInfo> desc = new StatementDescriptor<>(heapInfoCategory, QUERY_ALL_HEAPS);
        PreparedStatement<HeapInfo> stmt;
        Cursor<HeapInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, agentId.get());
            stmt.setString(1, vmId.get());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            log.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            log.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }

        return asList(cursor);
    }

    @Override
    public InputStream getHeapDumpData(HeapInfo heapInfo) {
        return storage.loadFile(heapInfo.getHeapDumpId());
    }

    @Override
    public ObjectHistogram getHistogram(HeapInfo heapInfo) {
        
        try (InputStream in = storage.loadFile(heapInfo.getHistogramId())) {
            ObjectInputStream ois = new ObjectInputStream(in);
            return (ObjectHistogram) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "Unexpected error while reading histogram", e);
            return null;
        }
    }

    @Override
    public HeapInfo getHeapInfo(String heapId) {
        StatementDescriptor<HeapInfo> desc = new StatementDescriptor<>(heapInfoCategory, QUERY_HEAP_INFO);
        PreparedStatement<HeapInfo> stmt; 
        Cursor<HeapInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, heapId);
            cursor = stmt.executeQuery();
        } catch (IllegalArgumentException iae) {
            /*
             * if the heap id is not found, we get a nice
             * IllegalArgumentException but check if the illegal argument
             * exception is caused by that before propagating it.
             */
            if (!iae.getMessage().contains("invalid ObjectId")) {
                // FIXME Is this needed?
                throw iae;
            }
            return null;
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            log.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            log.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        
        return head(cursor);
    }

    @Override
    public HeapDump getHeapDump(HeapInfo heapInfo) {
        return new HeapDump(heapInfo, this);
    }

}

