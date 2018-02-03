/**
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
 */
package org.dkpro.tc.ml.svmhmm.task.serialization;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.collections.BidiMap;
import org.codehaus.plexus.util.FileUtils;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.ModelSerializationTask;
import org.dkpro.tc.core.util.SaveModelUtils;
import org.dkpro.tc.ml.svmhmm.SVMHMMAdapter;
import org.dkpro.tc.ml.svmhmm.task.SVMHMMTestTask;
import org.dkpro.tc.ml.svmhmm.util.SVMHMMUtils;

public class SvmhmmModelSerializationDescription
    extends ModelSerializationTask
    implements Constants
{

    @Discriminator(name = DIM_CLASSIFICATION_ARGS)
    private List<String> classificationArguments;

    private double paramC;
    private double paramEpsilon;
    private int paramOrderE;
    private int paramOrderT;
    private int paramB;

    @Override
    public void execute(TaskContext aContext)
        throws Exception
    {
        trainAndStoreModel(aContext);
        writeModelConfiguration(aContext);
    }

    private void trainAndStoreModel(TaskContext aContext)
        throws Exception
    {

        processParameters(classificationArguments);

        File trainingDataStorage = aContext.getFolder(TEST_TASK_INPUT_KEY_TRAINING_DATA,
                StorageService.AccessMode.READONLY);

        File trainingFile = new File(trainingDataStorage, Constants.FILENAME_DATA_IN_CLASSIFIER_FORMAT);

        SortedSet<String> outcomeLabels = SVMHMMUtils
                .extractOutcomeLabelsFromFeatureVectorFiles(trainingFile);
        BidiMap labelsToIntegersMapping = SVMHMMUtils.mapVocabularyToIntegers(outcomeLabels);
        
        //copy feature names
        FileUtils.copyFile(new File(trainingDataStorage, FILENAME_FEATURES), new File(outputFolder, FILENAME_FEATURES));

        // // save mapping to file
        File mappingFile = new File(
                outputFolder.toString() + "/" + SVMHMMUtils.LABELS_TO_INTEGERS_MAPPING_FILE_NAME);
        SVMHMMUtils.saveMapping(labelsToIntegersMapping, mappingFile);

        File augmentedTrainingFile = SVMHMMUtils.replaceLabelsWithIntegers(trainingFile,
                labelsToIntegersMapping);

        File classifier = new File(outputFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        // train the model
        new SVMHMMTestTask().trainModel(classifier, augmentedTrainingFile, paramC, paramOrderE,
                paramOrderT, paramEpsilon, paramB);
    }

    private void processParameters(List<String> classificationArguments)
    {
        if (classificationArguments == null) {
            paramC = 5.0;
            paramEpsilon = 0.5;
            paramOrderT = 1;
            paramOrderE = 0;
            paramB = 0;
            return;
        }

        paramC = SVMHMMUtils.getParameterC(classificationArguments);
        paramEpsilon = SVMHMMUtils.getParameterEpsilon(classificationArguments);
        paramOrderE = SVMHMMUtils.getParameterOrderE_dependencyOfEmissions(classificationArguments);
        paramOrderT = SVMHMMUtils
                .getParameterOrderT_dependencyOfTransitions(classificationArguments);
        paramB = SVMHMMUtils.getParameterBeamWidth(classificationArguments);
    }

	@Override
	protected void writeAdapter() throws Exception {
		SaveModelUtils.writeModelAdapterInformation(outputFolder, SVMHMMAdapter.class.getName());
	}

}
