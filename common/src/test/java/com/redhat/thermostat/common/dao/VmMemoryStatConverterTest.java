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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmMemoryStatConverterTest {

    @Test
    public void testVmMemoryStatToChunk() {
        List<Generation> generations = new ArrayList<Generation>();

        int i = 0;
        for (String genName: new String[] { "new", "old", "perm" }) {
            Generation gen = new Generation();
            gen.name = genName;
            gen.collector = gen.name;
            generations.add(gen);
            List<Space> spaces = new ArrayList<Space>();
            gen.spaces = spaces;
            String[] spaceNames = null;
            if (genName.equals("new")) {
                spaceNames = new String[] { "eden", "s0", "s1" };
            } else if (genName.equals("old")) {
                spaceNames = new String[] { "old" };
            } else {
                spaceNames = new String[] { "perm" };
            }
            for (String spaceName: spaceNames) {
                Space space = new Space();
                space.name = spaceName;
                space.index = 0;
                space.used = i++;
                space.capacity = i++;
                space.maxCapacity = i++;
                spaces.add(space);
            }
        }

        VmMemoryStat stat = new VmMemoryStat(1, 2, generations);

        Chunk chunk = new VmMemoryStatConverter().vmMemoryStatToChunk(stat);

        assertNotNull(chunk);
        assertEquals((Long) 1l, chunk.get(new Key<Long>("timestamp", false)));
        assertEquals((Integer) 2, chunk.get(new Key<Integer>("vm-id", false)));
        assertEquals("new", chunk.get(new Key<String>("eden.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("eden.collector", false)));
        assertEquals((Long) 0l, chunk.get(new Key<Long>("eden.used", false)));
        assertEquals((Long) 1l, chunk.get(new Key<Long>("eden.capacity", false)));
        assertEquals((Long) 2l, chunk.get(new Key<Long>("eden.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.collector", false)));
        assertEquals((Long) 3l, chunk.get(new Key<Long>("s0.used", false)));
        assertEquals((Long) 4l, chunk.get(new Key<Long>("s0.capacity", false)));
        assertEquals((Long) 5l, chunk.get(new Key<Long>("s0.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.collector", false)));
        assertEquals((Long) 6l, chunk.get(new Key<Long>("s1.used", false)));
        assertEquals((Long) 7l, chunk.get(new Key<Long>("s1.capacity", false)));
        assertEquals((Long) 8l, chunk.get(new Key<Long>("s1.max-capacity", false)));
        assertEquals("old", chunk.get(new Key<String>("old.gen", false)));
        assertEquals("old", chunk.get(new Key<String>("old.collector", false)));
        assertEquals((Long) 9l, chunk.get(new Key<Long>("old.used", false)));
        assertEquals((Long) 10l, chunk.get(new Key<Long>("old.capacity", false)));
        assertEquals((Long) 11l, chunk.get(new Key<Long>("old.max-capacity", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.gen", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.collector", false)));
        assertEquals((Long) 12l, chunk.get(new Key<Long>("perm.used", false)));
        assertEquals((Long) 13l, chunk.get(new Key<Long>("perm.capacity", false)));
        assertEquals((Long) 14l, chunk.get(new Key<Long>("perm.max-capacity", false)));

    }

    @Test
    public void testDBObjectToVmMemoryStat() {
        final long TIMESTAMP = 1234l;
        final int VM_ID = 4567;

        BasicDBObject dbObject = new BasicDBObject();

        dbObject.put("timestamp", TIMESTAMP);
        dbObject.put("vm-id", VM_ID);
        BasicDBObject eden = new BasicDBObject();
        eden.put("gen", "new");
        eden.put("collector", "new-collector");
        eden.put("used", 1l);
        eden.put("capacity", 2l);
        eden.put("max-capacity", 3l);
        dbObject.put("eden", eden);

        BasicDBObject s0 = new BasicDBObject();
        s0.put("gen", "new");
        s0.put("collector", "new-collector");
        s0.put("used", 4l);
        s0.put("capacity", 5l);
        s0.put("max-capacity", 6l);
        dbObject.put("s0", s0);

        BasicDBObject s1 = new BasicDBObject();
        s1.put("gen", "new");
        s1.put("collector", "new-collector");
        s1.put("used", 7l);
        s1.put("capacity", 8l);
        s1.put("max-capacity", 9l);
        dbObject.put("s1", s1);

        BasicDBObject old = new BasicDBObject();
        old.put("gen", "old");
        old.put("collector", "old-collector");
        old.put("used", 10l);
        old.put("capacity", 11l);
        old.put("max-capacity", 12l);
        dbObject.put("old", old);

        BasicDBObject perm = new BasicDBObject();
        perm.put("gen", "perm");
        perm.put("collector", "perm-collector");
        perm.put("used", 13l);
        perm.put("capacity", 14l);
        perm.put("max-capacity", 15l);
        dbObject.put("perm", perm);

        VmMemoryStat stat = new VmMemoryStatConverter().createVmMemoryStatFromDBObject(dbObject);

        assertNotNull(stat);
        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(VM_ID, stat.getVmId());

        assertEquals(3, stat.getGenerations().size());

        assertEquals(3, stat.getGeneration("new").spaces.size());
        assertEquals("new-collector", stat.getGeneration("new").collector);

        assertEquals(1, stat.getGeneration("old").spaces.size());
        assertEquals("old-collector", stat.getGeneration("old").collector);

        assertEquals(1, stat.getGeneration("perm").spaces.size());
        assertEquals("perm-collector", stat.getGeneration("perm").collector);
    }

}
