package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bson.BSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.MemoryStat;

public class MongoStorage implements Storage {

    private Mongo mongo = null;
    private DB db = null;

    private UUID agentId = null;

    @Override
    public void connect(String uri) throws UnknownHostException {
        connect(new MongoURI(uri));
    }

    public void connect(MongoURI uri) throws UnknownHostException {
        mongo = new Mongo(uri);
        db = mongo.getDB(StorageConstants.THERMOSTAT_DB);
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public void addAgentInformation(StartupConfiguration config) {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        DBObject toInsert = config.toDBObject();
        /* cast required to disambiguate between putAll(BSONObject) and putAll(Map) */
        toInsert.putAll((BSONObject) getAgentDBObject());
        configCollection.insert(toInsert, WriteConcern.SAFE);
    }

    @Override
    public void removeAgentInformation() {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        BasicDBObject toRemove = getAgentDBObject();
        configCollection.remove(toRemove, WriteConcern.NORMAL);
    }

    @Override
    public String getBackendConfig(String backendName, String configurationKey) {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        BasicDBObject query = getAgentDBObject();
        query.put(StorageConstants.KEY_AGENT_CONFIG_BACKENDS + "." + backendName, new BasicDBObject("$exists", true));
        DBObject config = configCollection.findOne(query);
        Object value = config.get(configurationKey);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    private BasicDBObject getAgentDBObject() {
        return new BasicDBObject(StorageConstants.KEY_AGENT_ID, agentId.toString());
    }

    @Override
    public void updateHostInfo(HostInfo hostInfo) {
        /*
         * Host Info is determined (completely) on the agent side. No one else
         * should be touching it. So let's overwrite any changes to it.
         */
        DBObject queryForOldObject = getAgentDBObject();

        BasicDBObject toInsert = new BasicDBObject();
        toInsert.put(StorageConstants.KEY_AGENT_ID, agentId.toString());
        toInsert.put(StorageConstants.KEY_HOST_INFO_HOSTNAME, hostInfo.getHostname());

        BasicDBObject osParts = new BasicDBObject();
        osParts.put(StorageConstants.KEY_HOST_INFO_OS_NAME, hostInfo.getOsName());
        osParts.put(StorageConstants.KEY_HOST_INFO_OS_KERNEL, hostInfo.getOsKernel());
        toInsert.put(StorageConstants.KEY_HOST_INFO_OS, osParts);

        BasicDBObject cpuParts = new BasicDBObject();
        cpuParts.put(StorageConstants.KEY_HOST_INFO_CPU_COUNT, hostInfo.getCpuCount());
        toInsert.put(StorageConstants.KEY_HOST_INFO_CPU, cpuParts);

        BasicDBObject memoryParts = new BasicDBObject();
        memoryParts.put(StorageConstants.KEY_HOST_INFO_MEMORY_TOTAL, hostInfo.getTotalMemory());
        toInsert.put(StorageConstants.KEY_HOST_INFO_MEMORY, memoryParts);

        BasicDBObject networkParts = new BasicDBObject();
        for (Entry<String, List<String>> entry: hostInfo.getNetworkInfo().entrySet()) {
            BasicDBObject details = new BasicDBObject();
            details.put(StorageConstants.KEY_HOST_INFO_NETWORK_ADDR_IPV4,
                    entry.getValue().get(Constants.HOST_INFO_NETWORK_IPV4_INDEX));
            details.put(StorageConstants.KEY_HOST_INFO_NETWORK_ADDR_IPV6,
                    entry.getValue().get(Constants.HOST_INFO_NETWORK_IPV6_INDEX));
            networkParts.put(entry.getKey(), details);
        }
        toInsert.put(StorageConstants.KEY_HOST_INFO_NETWORK, networkParts);

        DBCollection hostInfoCollection = db.getCollection(StorageConstants.COLLECTION_HOST_INFO);
        hostInfoCollection.update(
                queryForOldObject,
                toInsert,
                true,
                false, /* doesnt matter should only have one object */
                WriteConcern.NORMAL
        );
    }

    @Override
    public void addCpuStat(CpuStat stat) {
        DBCollection cpuStatsCollection = db.getCollection(StorageConstants.COLLECTION_CPU_STATS);
        BasicDBObject toInsert = getAgentDBObject();
        toInsert.put(StorageConstants.KEY_TIMESTAMP, stat.getTimeStamp());
        toInsert.put(StorageConstants.KEY_CPU_STATS_LOAD, stat.getLoad());
        cpuStatsCollection.insert(toInsert, WriteConcern.NORMAL);
    }

    @Override
    public void addMemoryStat(MemoryStat stat) {
        DBCollection memoryStatsCollection = db.getCollection(StorageConstants.COLLECTION_MEMORY_STATS);
        BasicDBObject toInsert = getAgentDBObject();
        toInsert.put(StorageConstants.KEY_TIMESTAMP, stat.getTimeStamp());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_TOTAL, stat.getTotal());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_FREE, stat.getFree());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_BUFFERS, stat.getBuffers());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_CACHED, stat.getCached());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_SWAP_TOTAL, stat.getSwapTotal());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_SWAP_FREE, stat.getSwapFree());
        toInsert.put(StorageConstants.KEY_MEMORY_STATS_COMMIT_LIMIT, stat.getCommitLimit());
        memoryStatsCollection.insert(toInsert, WriteConcern.NORMAL);
    }
}
