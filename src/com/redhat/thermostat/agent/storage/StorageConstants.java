package com.redhat.thermostat.agent.storage;

public class StorageConstants {
    public static final String THERMOSTAT_DB = "thermostat";

    public static final String COLLECTION_AGENT_CONFIG = "agent-config";
    public static final String COLLECTION_HOST_INFO = "host-info";
    public static final String COLLECTION_CPU_STATS = "cpu-stats";
    public static final String COLLECTION_MEMORY_STATS = "memory-stats";

    public static final String KEY_AGENT_ID = "agent-id";
    public static final String KEY_TIMESTAMP = "timestamp";

    public static final String KEY_AGENT_CONFIG_BACKENDS = "backends";
    public static final String KEY_AGENT_CONFIG_AGENT_START_TIME = "start-time";

    public static final String KEY_HOST_INFO_HOSTNAME = "hostname";
    public static final String KEY_HOST_INFO_OS = "os";
    public static final String KEY_HOST_INFO_OS_NAME = "name";
    public static final String KEY_HOST_INFO_OS_KERNEL = "kernel";
    public static final String KEY_HOST_INFO_CPU = "cpu";
    public static final String KEY_HOST_INFO_CPU_COUNT = "num";
    public static final String KEY_HOST_INFO_MEMORY = "memory";
    public static final String KEY_HOST_INFO_MEMORY_TOTAL = "total";
    public static final String KEY_HOST_INFO_NETWORK = "network";
    public static final String KEY_HOST_INFO_NETWORK_ADDR_IPV4 = "ipv4addr";
    public static final String KEY_HOST_INFO_NETWORK_ADDR_IPV6 = "ipv6addr";

    public static final String KEY_CPU_STATS_LOAD = "load";

    public static final String KEY_MEMORY_STATS_TOTAL = "total";
    public static final String KEY_MEMORY_STATS_FREE = "free";
    public static final String KEY_MEMORY_STATS_BUFFERS = "buffers";
    public static final String KEY_MEMORY_STATS_CACHED = "cached";
    public static final String KEY_MEMORY_STATS_SWAP_TOTAL = "swap-total";
    public static final String KEY_MEMORY_STATS_SWAP_FREE = "swap-free";
    public static final String KEY_MEMORY_STATS_COMMIT_LIMIT = "commit-limit";

}
