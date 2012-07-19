package com.redhat.thermostat.client.ui;

import com.redhat.thermostat.client.osgi.service.Filter;
import com.redhat.thermostat.common.dao.Ref;

public class HostVmFilter implements Filter {
    
    private String filter;

    public HostVmFilter() {
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public boolean matches(Ref ref) {
        if (filter == null || filter.isEmpty()) {
            return true;
            
        } else {
            return matches(ref, filter);                
        }
    }

    public boolean matches(Ref ref, String filter) {
      return ref.getName().contains(filter) || ref.getStringID().contains(filter);
    }
}