package com.redhat.thermostat.client;

import static com.redhat.thermostat.client.Translate._;

public enum MemoryType {
    MEMORY_TOTAL("total", _("HOST_MEMORY_TOTAL")),
    MEMORY_FREE("free", _("HOST_MEMORY_FREE")),
    MEMORY_USED("used", _("HOST_MEMORY_USED")),
    SWAP_TOTAL("swap-total", _("HOST_SWAP_TOTAL")),
    SWAP_FREE("swap-free", _("HOST_SWAP_FREE")),
    SWAP_BUFFERS("swap-buffers", _("HOST_BUFFERS"));

    private String humanReadable;

    private MemoryType(String key, String humanReadable) {
        this.humanReadable = humanReadable;
    }

    public String getLabel() {
        return humanReadable;
    }

    @Override
    public String toString() {
        return humanReadable;
    }

}
