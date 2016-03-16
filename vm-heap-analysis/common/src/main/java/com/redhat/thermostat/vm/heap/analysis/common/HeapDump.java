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

package com.redhat.thermostat.vm.heap.analysis.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.Snapshot;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.parser.Reader;

/**
 * NOTE: This class is thread-safe with respect to loading the heapdump and creating the index.
 */
public class HeapDump {

    private static final String INDEX_FIELD_OBJECT_ID = "objectId";

    private static final String INDEX_FIELD_CLASSNAME = "classname";

    private static final Logger log = LoggingUtils.getLogger(HeapDump.class);

    private static final int MAX_SEARCH_RESULTS = 1000;

    private final HeapInfo heapInfo;

    private final HeapDAO heapDAO;

    private Snapshot snapshot;

    private Directory luceneIndex;

    // package-private for testing
    HeapDump(HeapInfo heapInfo, HeapDAO heapDAO, Snapshot snapshot) {
        this.heapInfo = heapInfo;
        this.heapDAO = heapDAO;
        this.snapshot = snapshot;
    }
    
    public HeapDump(HeapInfo heapInfo, HeapDAO heapDAO) {
        this(heapInfo, heapDAO, null);
    }

    public long getTimestamp() {
        return heapInfo.getTimeStamp();
    }

    @Override
    public String toString() {
        return "[" + new Date(getTimestamp()) +"] ";
    }

    public ObjectHistogram getHistogram() throws IOException {
        return heapDAO.getHistogram(heapInfo);
    }

    public HeapInfo getInfo() {
        return heapInfo;
    }

    private synchronized Directory getLuceneIndex() {
        if (luceneIndex == null) {
            try {
                luceneIndex = createLuceneIndex();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Unexpected IO Exception while creating heap dump index", ex);
                return null;
            }
        }
        return luceneIndex;
    }

    // package-private for testing
    Directory createLuceneIndex() throws IOException,
            CorruptIndexException, LockObtainFailedException {

        loadHeapDumpIfNecessary();

        Enumeration<JavaHeapObject> thingos = snapshot.getThings();
        Directory dir = new RAMDirectory();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new SimpleAnalyzer());
        IndexWriter writer = new IndexWriter(dir, indexWriterConfig);
        while (thingos.hasMoreElements()) {
            JavaHeapObject thingo = thingos.nextElement();
            Document doc = new Document();
            doc.add(new StringField(INDEX_FIELD_CLASSNAME, thingo.getClazz().getName(), Field.Store.YES));
            doc.add(new StringField(INDEX_FIELD_OBJECT_ID, thingo.getIdString(), Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.close();
        return dir;
    }

    public Snapshot getSnapshot() {
        loadHeapDumpIfNecessary();
        return snapshot;
    }

    private synchronized void loadHeapDumpIfNecessary() {
        if (snapshot == null) {
            try {
                loadHeapDump();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Unexpected IO Exception while loading heap dump", e);
            }
        }
    }

    private void loadHeapDump() throws IOException {
        String filename = "heapdump-" + heapInfo.getHeapId();
        File tmpDir = getOrCreateHeapDumpDir();
        File tmpFile = new File(tmpDir, filename);
        if (! tmpFile.exists()) {
            try (InputStream in = heapDAO.getHeapDumpData(heapInfo);) {
                Files.copy(in, tmpFile.toPath());
            }
            
        }
        snapshot = Reader.readFile(tmpFile.getAbsolutePath(), true, 0);
        snapshot.resolve(true);
    }

    private File getOrCreateHeapDumpDir() throws IOException {
        String dirname = "thermostat-" + System.getProperty("user.name");
        File tmpFile = new File(System.getProperty("java.io.tmpdir"), dirname);
        if (! tmpFile.exists()) {
            Files.createDirectory(tmpFile.toPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            return tmpFile;
        } else {
            if (tmpFile.isDirectory()) {
                return tmpFile;
            } else {
                throw new FileAlreadyExistsException(tmpFile.getAbsolutePath());
            }
        }
    }

    /**
     * Find objects with class names matching the given pattern
     * @param wildCardClassNamePattern a case-sensitive wildcard pattern to match class names against
     * @param limit the maximum number of results to return
     * @return a collection of object ids that can be used with {@link #findObject(String)}
     */
    public Collection<String> searchObjects(String wildCardClassNamePattern, int limit) {
        Directory searchIndex = getLuceneIndex();

        WildcardQuery query = new WildcardQuery(new Term(INDEX_FIELD_CLASSNAME, wildCardClassNamePattern));
        Collection<String> results = new ArrayList<String>();
        try {
            IndexReader indexReader = DirectoryReader.open(searchIndex);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs found = searcher.search(query, limit);
            for (ScoreDoc scoreDoc : found.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String objectId = doc.get(INDEX_FIELD_OBJECT_ID);
                results.add(objectId);
            }
            indexReader.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unexpected IO Exception while searching heap dump index", e);
        }
        return results;
    }

    public Collection<String> wildcardSearch(String searchText) {
        int limit = Math.min(MAX_SEARCH_RESULTS, searchText.length() * 100);

        String wildCardClassNamePattern = searchText;
        if (!searchText.contains("*") && !searchText.contains("?")) {
            wildCardClassNamePattern = "*" + searchText + "*";
        }
        return searchObjects(wildCardClassNamePattern, limit);
    }

    public JavaHeapObject findObject(String id) {
        loadHeapDumpIfNecessary();
        return snapshot.findThing(id);
    }

    public boolean equals(Object o) {
        return o instanceof HeapDump && ((HeapDump) o).heapInfo.equals(heapInfo);
    }

    public int hashCode() {
        return heapInfo.hashCode();
    }
    
    public String getType() {
        return "hprof";
    }
}

