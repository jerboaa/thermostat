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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.cli.CompletionFinder;
import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HeapIdsFinder implements CompletionFinder {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private DependencyServices dependencyServices;

    public HeapIdsFinder(DependencyServices dependencyServices) {
        this.dependencyServices = dependencyServices;
    }

    @Override
    public List<CompletionInfo> findCompletions() {
        if (!(dependencyServices.hasService(HeapDAO.class) && dependencyServices.hasService(VmInfoDAO.class))) {
            return Collections.emptyList();
        }

        HeapDAO heapDao = dependencyServices.getService(HeapDAO.class);
        VmInfoDAO vmDao = dependencyServices.getService(VmInfoDAO.class);

        List<CompletionInfo> heapIds = new ArrayList<>();
        for (HeapInfo heap : heapDao.getAllHeapInfo()) {
            CompletionInfo completionInfo = getCompletionInfo(vmDao, heap);
            heapIds.add(completionInfo);
        }
        return heapIds;
    }

    private CompletionInfo getCompletionInfo(VmInfoDAO vmDao, HeapInfo heap) {
        VmInfo vmInfo = vmDao.getVmInfo(new VmId(heap.getVmId()));

        String mainclass = vmInfo.getMainClass();
        String timestamp = Clock.DEFAULT_DATE_FORMAT.format(new Date(heap.getTimeStamp()));

        String userVisibleText = t.localize(LocaleResources.HEAPID_COMPLETION_WITH_USER_TEXT, mainclass, timestamp).getContents();
        return new CompletionInfo(heap.getHeapId(), userVisibleText);
    }

}
