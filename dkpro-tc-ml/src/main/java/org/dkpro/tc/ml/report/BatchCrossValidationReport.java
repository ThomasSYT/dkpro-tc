/*******************************************************************************
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.tc.ml.report;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.dkpro.lab.reporting.BatchReportBase;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.TcTaskTypeUtil;
import org.dkpro.tc.core.util.ReportUtils;
import org.dkpro.tc.core.util.TcFlexTable;
import org.dkpro.tc.ml.report.util.MetricComputationUtil;

/**
 * Collects the final evaluation results in a cross validation setting.
 * 
 */
public class BatchCrossValidationReport extends BatchReportBase implements Constants {
	boolean softEvaluation = true;
	boolean individualLabelMeasures = false;

	public BatchCrossValidationReport() {
		// required by groovy
	}

	@Override
	public void execute() throws Exception {

		StorageService store = getContext().getStorageService();

		TcFlexTable<String> table = TcFlexTable.forClass(String.class);

		for (TaskContextMetadata subcontext : getSubtasks()) {
			if (!TcTaskTypeUtil.isCrossValidationTask(store, subcontext.getId())) {
				continue;
			}

			File combinedId2outcome = store.locateKey(subcontext.getId(),
					Constants.COMBINED_ID_OUTCOME_KEY);

			Map<String, String> discriminatorsMap = ReportUtils.getDiscriminatorsForContext(store, subcontext.getId(),
					Task.DISCRIMINATORS_KEY);
			
			String learningMode = null;
			for (String key : discriminatorsMap.keySet()) {
				if (key.endsWith("|" + DIM_LEARNING_MODE)) {
					learningMode = discriminatorsMap.get(key);
					break;
				}
			}

			Map<String, String> results = MetricComputationUtil.getResults(combinedId2outcome, learningMode);

			Map<String, String> values = new HashMap<String, String>();
			values.putAll(discriminatorsMap);
			values.putAll(results);

			table.addRow(subcontext.getLabel(), values);
		}

		/*
		 * TODO: make rows to columns e.g. create a new table and set columns to
		 * rows of old table and rows to columns but than must be class
		 * FlexTable in this case adapted accordingly: enable setting
		 */

		ReportUtils.writeExcelAndCSV(getContext(), getContextLabel(), table, EVAL_FILE_NAME, SUFFIX_EXCEL, SUFFIX_CSV);
	}

}
