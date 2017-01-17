/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileDAOImpl extends AbstractDao implements ProfileDAO {

    private static final Logger logger = LoggingUtils.getLogger(ProfileDAOImpl.class);

    private static final Key<Long> KEY_START_TIME_STAMP = new Key<>("startTimeStamp");
    private static final Key<Long> KEY_STOP_TIME_STAMP = new Key<>("stopTimeStamp");

    private static final Key<String> KEY_PROFILE_ID = new Key<>("profileId");

    static final Category<ProfileInfo> PROFILE_INFO_CATEGORY = new Category<>(
            "profile-info",
            ProfileInfo.class,
            Key.AGENT_ID, Key.VM_ID, KEY_START_TIME_STAMP, KEY_STOP_TIME_STAMP, KEY_PROFILE_ID);

    static final String PROFILE_INFO_DESC_ADD = ""
            + "ADD " + PROFILE_INFO_CATEGORY.getName() + " SET "
            + " '" + Key.AGENT_ID.getName() + "' = ?s ,"
            + " '" + Key.VM_ID.getName() + "' = ?s ,"
            + " '" + KEY_START_TIME_STAMP.getName() + "' = ?l ,"
            + " '" + KEY_STOP_TIME_STAMP.getName() + "' = ?l ,"
            + " '" + KEY_PROFILE_ID.getName() + "' = ?s";

    static final String PROFILE_INFO_DESC_QUERY_LATEST = "QUERY "
            + PROFILE_INFO_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + KEY_STOP_TIME_STAMP.getName() + "' DSC LIMIT 1";

    static final String PROFILE_INFO_DESC_QUERY_BY_ID = "QUERY "
            + PROFILE_INFO_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + KEY_STOP_TIME_STAMP.getName() + "' = ?s LIMIT 1";

    static final String PROFILE_INFO_DESC_INTERVAL_QUERY = "QUERY "
            + PROFILE_INFO_CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + KEY_STOP_TIME_STAMP.getName() + "' >= ?l AND '"
            + KEY_STOP_TIME_STAMP.getName() + "' < ?l SORT '"
            + KEY_STOP_TIME_STAMP.getName() + "' DSC";

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

    public ProfileDAOImpl(Storage storage) {
        this.storage = storage;
        this.storage.registerCategory(PROFILE_INFO_CATEGORY);
        this.storage.registerCategory(PROFILE_STATUS_CATEGORY);
    }

    @Override
    public void saveProfileData(final ProfileInfo info, final InputStream data, final Runnable whenDone) {
        storage.saveFile(info.getProfileId(), data, new SaveFileListener() {

            @Override
            public void notify(EventType type, Object additionalArguments) {
                switch (type) {
                case EXCEPTION_OCCURRED:
                    StorageException cause = (StorageException) additionalArguments;
                    cause.printStackTrace();
                    whenDone.run();
                    break;
                case SAVE_COMPLETE:
                    addProfileInfoToStorage(info);
                    whenDone.run();
                    break;
                default:
                    logger.log(Level.WARNING, "Unknown saveFile event: " + type);
                }
            }
        });

    }

    private void addProfileInfoToStorage(final ProfileInfo info) {
        executeStatement(new AbstractDaoStatement<ProfileInfo>(storage, PROFILE_INFO_CATEGORY, PROFILE_INFO_DESC_ADD) {
            @Override
            public PreparedStatement<ProfileInfo> customize(PreparedStatement<ProfileInfo> preparedStatement) {
                preparedStatement.setString(0, info.getAgentId());
                preparedStatement.setString(1, info.getVmId());
                preparedStatement.setLong(2, info.getStartTimeStamp());
                preparedStatement.setLong(3, info.getStopTimeStamp());
                preparedStatement.setString(4, info.getProfileId());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<ProfileInfo> getAllProfileInfo(VmRef vm, Range<Long> timeRange) {
        return getAllProfileInfo(new AgentId(vm.getHostRef().getAgentId()), new VmId(vm.getVmId()), timeRange);
    }

    @Override
    public List<ProfileInfo> getAllProfileInfo(final AgentId agentId, final VmId vmId, final Range<Long> timeRange) {
        return executeQuery(new AbstractDaoQuery<ProfileInfo>(storage, PROFILE_INFO_CATEGORY, PROFILE_INFO_DESC_INTERVAL_QUERY) {
            @Override
            public PreparedStatement<ProfileInfo> customize(PreparedStatement<ProfileInfo> preparedStatement) {
                preparedStatement.setString(0, agentId.get());
                preparedStatement.setString(1, vmId.get());
                preparedStatement.setLong(2, timeRange.getMin());
                preparedStatement.setLong(3, timeRange.getMax());
                return preparedStatement;
            }
        }).asList();
    }

    @Override
    public InputStream loadProfileDataById(VmRef vm, String profileId) {
        return getProfileData(profileId);
    }

    @Override
    public InputStream loadLatestProfileData(AgentId agentId, VmId vmId) {
        ProfileInfo info = loadLatest(agentId, vmId, PROFILE_INFO_CATEGORY, PROFILE_INFO_DESC_QUERY_LATEST);
        if (info == null) {
            return null;
        }

        return getProfileData(info.getProfileId());
    }

    private InputStream getProfileData(String profileId) {
        return storage.loadFile(profileId);
    }

    @Override
    public void addStatus(final ProfileStatusChange status) {
        executeStatement(new AbstractDaoStatement<ProfileStatusChange>(storage, PROFILE_STATUS_CATEGORY, PROFILE_STATUS_DESC_ADD) {
            @Override
            public PreparedStatement<ProfileStatusChange> customize(PreparedStatement<ProfileStatusChange> preparedStatement) {
                preparedStatement.setString(0, status.getAgentId());
                preparedStatement.setString(1, status.getVmId());
                preparedStatement.setLong(2, status.getTimeStamp());
                preparedStatement.setBoolean(3, status.isStarted());
                return preparedStatement;
            }
        });
    }

    @Override
    public ProfileStatusChange getLatestStatus(VmRef vm) {
        return getLatestStatus(new AgentId(vm.getHostRef().getAgentId()), new VmId(vm.getVmId()));
    }

    @Override
    public ProfileStatusChange getLatestStatus(AgentId agentId, VmId vmId) {
        return loadLatest(agentId, vmId, PROFILE_STATUS_CATEGORY, PROFILE_STATUS_DESC_QUERY_LATEST);
    }

    private <T extends BasePojo> T loadLatest(final AgentId agentId, final VmId vmId, Category<T> category, String queryDesc) {
        return executeQuery(new AbstractDaoQuery<T>(storage, category, queryDesc) {
            @Override
            public PreparedStatement<T> customize(PreparedStatement<T> prepared) {
                prepared.setString(0, agentId.get());
                prepared.setString(1, vmId.get());
                return prepared;
            }
        }).head();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
