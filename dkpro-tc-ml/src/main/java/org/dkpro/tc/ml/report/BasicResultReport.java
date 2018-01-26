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
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

import org.dkpro.lab.reporting.ReportBase;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.impl.PropertiesAdapter;
import org.dkpro.lab.task.Task;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.ExtractFeaturesTask;
import org.dkpro.tc.core.task.deep.VectorizationTask;
import org.dkpro.tc.ml.report.util.MetricComputationUtil;
import org.dkpro.tc.ml.report.util.SortedKeyProperties;

/**
 * A result report which creates a few basic measures and writes them to the output folder of a run to
 * provide by default at least some result values.
 */
public class BasicResultReport
    extends ReportBase
    implements Constants
{

    static String OUTPUT_FILE = "results.txt";

    @Override
    public void execute()
        throws Exception
    {
        StorageService store = getContext().getStorageService();

        File id2outcomeFile = getContext().getStorageService().locateKey(getContext().getId(),
                Constants.ID_OUTCOME_KEY);

        String learningMode = store
                .retrieveBinary(getContext().getId(), Task.DISCRIMINATORS_KEY,
                        new PropertiesAdapter())
                .getMap().get(ExtractFeaturesTask.class.getName() + "|" + DIM_LEARNING_MODE);
        
        if (learningMode == null) {
            learningMode = store
                    .retrieveBinary(getContext().getId(), Task.DISCRIMINATORS_KEY,
                            new PropertiesAdapter())
                    .getMap().get(VectorizationTask.class.getName() + "|" + DIM_LEARNING_MODE);
        }

        
        Properties pa = new SortedKeyProperties();
        Map<String, String> resultMap = MetricComputationUtil.getResults(id2outcomeFile, learningMode);
        for(String k : resultMap.keySet()){
        	pa.setProperty(k, resultMap.get(k));
        }

        File key = store.locateKey(getContext().getId(), OUTPUT_FILE);
        FileOutputStream fileOutputStream = new FileOutputStream(key);
        pa.store(fileOutputStream, "Results");
        fileOutputStream.close();
        
    }
 
}