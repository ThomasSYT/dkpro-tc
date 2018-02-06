/**
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.dkpro.tc.examples.util;

import java.io.File;

import org.dkpro.lab.reporting.BatchReportBase;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.TcTaskTypeUtil;

/**
 * This is a slightly ugly solution for recording the DKPro Lab output folder of
 * an experiment to read result files in JUnit tests
 */
public class ContextMemoryReport extends BatchReportBase {

	public static File id2outcome;

	@Override
	public void execute() throws Exception {
		for (TaskContextMetadata subcontext : getSubtasks()) {
			StorageService storageService = getContext().getStorageService();

			if (TcTaskTypeUtil.isMachineLearningAdapterTask(storageService, subcontext.getId())) {
				id2outcome = storageService.locateKey(subcontext.getId(), Constants.ID_OUTCOME_KEY);
				return;
			}
			if (TcTaskTypeUtil.isCrossValidationTask(storageService, subcontext.getId())) {
				id2outcome = storageService.locateKey(subcontext.getId(), Constants.COMBINED_ID_OUTCOME_KEY);
				return;
			}
		}
	}
}
