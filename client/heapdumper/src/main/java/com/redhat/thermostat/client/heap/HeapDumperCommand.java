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

package com.redhat.thermostat.client.heap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HeapInfo;

public class HeapDumperCommand {
    
    private static final Logger log = Logger.getLogger(HeapDumperCommand.class.getName());

    public HeapDump execute(VmRef reference) {

        try {
            File heapDumpFile = dumpHeap(reference);
            InputStream histogram = writeHistogram(reference);
            HeapInfo info = saveHeapDumpInfo(reference, heapDumpFile, histogram);
            
            HeapDump dump = new HeapDump();
            dump.setHeapInfo(info);
            
            return dump;
        
        } catch (IOException e) {
            
            log.log(Level.SEVERE, "Unexpected IO problem while writing heap dump", e);
            return null;
        }
    }

    private File dumpHeap(VmRef reference) throws IOException {
        File tempFile = Files.createTempFile("thermostat-", "-heapdump").toFile();
        String tempFileName = tempFile.getAbsolutePath();
        tempFile.delete(); // Need to delete before dumping heap, jmap does not override existing file and stop with an error.
        Process proc = Runtime.getRuntime().exec(new String[] {"jmap", "-dump:format=b,file=" + tempFileName, reference.getIdString()});
        try {
            proc.waitFor();
            log.info("Heap dump written to: " + tempFileName);
        
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return tempFile;
    }
    
    private InputStream writeHistogram(VmRef reference) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[] {"jmap", "-histo", reference.getIdString()});
        InputStream histogramStream = proc.getInputStream();
        return histogramStream;
    }

    private HeapInfo saveHeapDumpInfo(VmRef reference, File tempFile, InputStream histogram) throws FileNotFoundException {
    
        HeapDAO heapDAO = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();
        HeapInfo heapInfo = new HeapInfo(reference, System.currentTimeMillis());
        heapInfo.setHeapDumpId(reference.getStringID() + "-" + reference.getAgent().getAgentId() + "-" + heapInfo.getTimestamp());
        
        heapDAO.putHeapInfo(heapInfo, new FileInputStream(tempFile), histogram);
        
        return heapInfo;
    }
}
