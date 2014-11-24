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
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;

public class ProfileDAOImpl implements ProfileDAO {

    private static final Logger logger = LoggingUtils.getLogger(ProfileDAOImpl.class);

    private static final Key<String> KEY_PROFILE_ID = new Key<>("profileId");

    static final Category<ProfileInfo> CATEGORY = new Category<>(
            "profile-info",
            ProfileInfo.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, KEY_PROFILE_ID);

    static final String DESC_ADD_PROFILE_INFO = ""
            + "ADD " + CATEGORY.getName() + " SET "
            + " '" + Key.AGENT_ID.getName() + "' = ?s ,"
            + " '" + Key.VM_ID.getName() + "' = ?s ,"
            + " '" + Key.TIMESTAMP.getName() + "' = ?l ,"
            + " '" + KEY_PROFILE_ID.getName() + "' = ?s";

    static final String DESC_QUERY_LATEST = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String DESC_QUERY_BY_ID = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' = ?s LIMIT 1";

    // internal information of VmTimeIntervalPojoListGetter being leaked :(
    static final String DESC_INTERVAL_QUERY = String.format(
            VmTimeIntervalPojoListGetter.VM_INTERVAL_QUERY_FORMAT, ProfileDAOImpl.CATEGORY.getName());

    private final Storage storage;
    private final VmTimeIntervalPojoListGetter<ProfileInfo> getter;

    public ProfileDAOImpl(Storage storage) {
        this.storage = storage;
        this.storage.registerCategory(CATEGORY);

        this.getter = new VmTimeIntervalPojoListGetter<>(storage, CATEGORY);
    }

    @Override
    public void saveProfileData(ProfileInfo info, InputStream data) {
        storage.saveFile(info.getProfileId(), data);
        addProfileInfoToStorage(info);
    }

    private void addProfileInfoToStorage(ProfileInfo info) {
        StatementDescriptor<ProfileInfo> desc = new StatementDescriptor<>(CATEGORY, DESC_ADD_PROFILE_INFO);
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
        // TODO should we check whether this profileId is valid by querying the DB first?
        return getProfileData(profileId);
    }

    @Override
    public InputStream loadLatestProfileData(VmRef vm) {
        StatementDescriptor<ProfileInfo> desc = new StatementDescriptor<>(CATEGORY, DESC_QUERY_LATEST);
        PreparedStatement<ProfileInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, vm.getHostRef().getAgentId());
            prepared.setString(1, vm.getVmId());
            Cursor<ProfileInfo> cursor = prepared.executeQuery();
            if (!cursor.hasNext()) {
                return null;
            }
            ProfileInfo info = cursor.next();
            return getProfileData(info.getProfileId());
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
        return null;
    }

    private InputStream getProfileData(String profileId) {
        return storage.loadFile(profileId);
    }
}
