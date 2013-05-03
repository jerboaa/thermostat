package com.redhat.thermostat.web.server.auth;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;

/**
 * Class representing a thermostat role. It simply wraps an existing {@link Group}. 
 * 
 * @see Group
 */
public class WrappedRolePrincipal extends BasicRole {

    private static final long serialVersionUID = -3852507889428067737L;
    // the underlying group
    private final Group group;
    
    /**
     * Creates a role which delegates to the given group.
     * 
     * @param group
     */
    public WrappedRolePrincipal(Group group) {
        super(group.getName());
        this.group = group;
    }

    @Override
    public boolean addMember(Principal user) {
        return this.group.addMember(user);
    }

    @Override
    public boolean removeMember(Principal user) {
        return this.group.removeMember(user);
    }

    @Override
    public boolean isMember(Principal member) {
        return this.group.isMember(member);
    }

    @Override
    public Enumeration<? extends Principal> members() {
        return this.group.members();
    }

}
