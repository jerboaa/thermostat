/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.common.internal;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileDAOImpl implements ProfileDAO {

    private static final Logger logger = LoggingUtils.getLogger(ProfileDAOImpl.class);

    private static final Key<String> KEY_PROFILE_ID = new Key<>("profileId");

    static final Category<ProfileInfo> PROFILE_INFO_CATEGORY = new Category<>(
            "profile-info",
            ProfileInfo.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, KEY_PROFILE_ID);

    static final String PROFILE_INFO_DESC_ADD = ""
            + "ADD " + PROFILE_INFO_CATEGORY.getName() + " SET "
            + " '" + Key.AGENT_ID.getName() + "' = ?s ,"
            + " '" + Key.VM_ID.getName() + "' = ?s ,"
            + " '" + Key.TIMESTAMP.getName() + "' = ?l ,"
            + " '" + KEY_PROFILE_ID.getName() + "' = ?s";

    static final String PROFILE_INFO_DESC_QUERY_LATEST = "QUERY "
            + PROFILE_INFO_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String PROFILE_INFO_DESC_QUERY_BY_ID = "QUERY "
            + PROFILE_INFO_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' = ?s LIMIT 1";

    // internal information of VmTimeIntervalPojoListGetter being leaked :(
    static final String PROFILE_INFO_DESC_INTERVAL_QUERY = String.format(
            VmTimeIntervalPojoListGetter.VM_INTERVAL_QUERY_FORMAT, ProfileDAOImpl.PROFILE_INFO_CATEGORY.getName());

    private static final Key<Boolean> KEY_PROFILE_STARTED = new Key<>("started");

    static final Category<ProfileStatusChange> PROFILE_STATUS_CATEGORY = new Category<>(
            "profile-status",
            ProfileStatusChange.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, KEY_PROFILE_STARTED);

    static final String PROFILE_STATUS_DESC_ADD = ""
            + "ADD " + PROFILE_STATUS_CATEGORY.getName() + " SET "
            + " '" + Key.AGENT_ID.getName() + "' = ?s ,"
            + " '" + Key.VM_ID.getName() + "' = ?s ,"
            + " '" + Key.TIMESTAMP.getName() + "' = ?l ,"
            + " '" + KEY_PROFILE_STARTED.getName() + "' = ?b";

    static final String PROFILE_STATUS_DESC_QUERY_LATEST = "QUERY "
            + PROFILE_STATUS_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    private final Storage storage;
    private final VmTimeIntervalPojoListGetter<ProfileInfo> getter;

    public ProfileDAOImpl(Storage storage) {
        this.storage = storage;
        this.storage.registerCategory(PROFILE_INFO_CATEGORY);
        this.storage.registerCategory(PROFILE_STATUS_CATEGORY);

        this.getter = new VmTimeIntervalPojoListGetter<>(storage, PROFILE_INFO_CATEGORY);
    }

    @Override
    public void saveProfileData(ProfileInfo info, InputStream data) {
        storage.saveFile(info.getProfileId(), data);
        addProfileInfoToStorage(info);
    }

    private void addProfileInfoToStorage(ProfileInfo info) {
        StatementDescriptor<ProfileInfo> desc = new StatementDescriptor<>(PROFILE_INFO_CATEGORY, PROFILE_INFO_DESC_ADD);
        PreparedStatement<ProfileInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, info.getAgentId());
            prepared.setString(1, info.getVmId());
            prepared.setLong(2, info.getTimeStamp());
            prepared.setString(3, info.getProfileId());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public List<ProfileInfo> getAllProfileInfo(VmRef vm, Range<Long> timeRange) {
        System.out.println("ProfileDAOImpl: getAllProfileInfo()");
        return getter.getLatest(vm, timeRange.getMin(), timeRange.getMax());
    }

    @Override
    public InputStream loadProfileDataById(VmRef vm, String profileId) {
        return getProfileData(profileId);
    }

    @Override
    public InputStream loadLatestProfileData(VmRef vm) {
        ProfileInfo info = loadLatest(vm, PROFILE_INFO_CATEGORY, PROFILE_INFO_DESC_QUERY_LATEST);
        if (info == null) {
            return null;
        }

        return getProfileData(info.getProfileId());
    }

    private InputStream getProfileData(String profileId) {
        return storage.loadFile(profileId);
    }

    @Override
    public void addStatus(ProfileStatusChange status) {
        StatementDescriptor<ProfileStatusChange> desc = new StatementDescriptor<>(PROFILE_STATUS_CATEGORY, PROFILE_STATUS_DESC_ADD);
        PreparedStatement<ProfileStatusChange> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, status.getAgentId());
            prepared.setString(1, status.getVmId());
            prepared.setLong(2, status.getTimeStamp());
            prepared.setBoolean(3, status.isStarted());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public ProfileStatusChange getLatestStatus(VmRef vm) {
        return loadLatest(vm, PROFILE_STATUS_CATEGORY, PROFILE_STATUS_DESC_QUERY_LATEST);
    }

    private <T extends BasePojo> T loadLatest(VmRef vm, Category<T> category, String queryDesc) {
        StatementDescriptor<T> desc = new StatementDescriptor<>(category, queryDesc);
        PreparedStatement<T> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, vm.getHostRef().getAgentId());
            prepared.setString(1, vm.getVmId());
            Cursor<T> cursor = prepared.executeQuery();
            if (!cursor.hasNext()) {
                return null;
            }
            return cursor.next();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
        return null;
    }

}
