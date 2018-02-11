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
package org.dkpro.tc.ml.weka.task;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.ml.weka.util.MultilabelResult;
import org.dkpro.tc.ml.weka.util.WekaUtils;

import meka.core.Result;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Base class for test task and save model tasks
 */
public class WekaTestTask
    extends ExecutableTaskBase
    implements Constants
{

    public final static String featureSelectionFile = "featureSelection.txt";
    
	@Discriminator(name=DIM_CLASSIFICATION_ARGS)
    protected List<Object> classificationArguments;
	
    @Discriminator(name=DIM_FEATURE_SEARCHER_ARGS)
    protected List<String> featureSearcher;
    
    @Discriminator(name=DIM_ATTRIBUTE_EVALUATOR_ARGS)
    protected List<String> attributeEvaluator;
    
    @Discriminator(name=DIM_LABEL_TRANSFORMATION_METHOD)
    protected String labelTransformationMethod;
    
    @Discriminator(name=DIM_NUM_LABELS_TO_KEEP)
    protected int numLabelsToKeep;
    
    @Discriminator(name=DIM_APPLY_FEATURE_SELECTION)
    protected boolean applySelection;
    
    @Discriminator(name=DIM_FEATURE_MODE)
    protected String featureMode;
    
    @Discriminator(name=DIM_LEARNING_MODE)
    protected String learningMode;
    
    @Discriminator(name=DIM_BIPARTITION_THRESHOLD)
    protected String threshold;

    public static final String evaluationBin = "evaluation.bin";
    
    @Override
    public void execute(TaskContext aContext)
        throws Exception
    {
        boolean multiLabel = learningMode.equals(Constants.LM_MULTI_LABEL);

        File arffFileTrain = WekaUtils.getFile(aContext, TEST_TASK_INPUT_KEY_TRAINING_DATA,
        		Constants.FILENAME_DATA_IN_CLASSIFIER_FORMAT, AccessMode.READONLY);
        File arffFileTest = WekaUtils.getFile(aContext, TEST_TASK_INPUT_KEY_TEST_DATA,
        		Constants.FILENAME_DATA_IN_CLASSIFIER_FORMAT, AccessMode.READONLY);

        Instances trainData = WekaUtils.getInstances(arffFileTrain, multiLabel);
        Instances testData = WekaUtils.getInstances(arffFileTest, multiLabel);

        // do not balance in regression experiments
        if (!learningMode.equals(Constants.LM_REGRESSION)) {
            testData = WekaUtils.makeOutcomeClassesCompatible(trainData, testData, multiLabel);
        }

        Instances copyTestData = new Instances(testData);
        trainData = WekaUtils.removeInstanceId(trainData, multiLabel);
        testData = WekaUtils.removeInstanceId(testData, multiLabel);
      

        // FEATURE SELECTION
        if (!learningMode.equals(Constants.LM_MULTI_LABEL)) {
            if (featureSearcher != null && attributeEvaluator != null) {
                AttributeSelection attSel = WekaUtils.featureSelectionSinglelabel(aContext,
                        trainData, featureSearcher, attributeEvaluator);
                File file = WekaUtils.getFile(aContext, "",
                        WekaTestTask.featureSelectionFile, AccessMode.READWRITE);
                FileUtils.writeStringToFile(file, attSel.toResultsString(), "utf-8");
                if (applySelection) {
                    Logger.getLogger(getClass()).info("APPLYING FEATURE SELECTION");
                    trainData = attSel.reduceDimensionality(trainData);
                    testData = attSel.reduceDimensionality(testData);
                }
            }
        }
        else {
            if (attributeEvaluator != null && labelTransformationMethod != null
                    && numLabelsToKeep > 0) {
                Remove attSel = WekaUtils.featureSelectionMultilabel(aContext, trainData,
                        attributeEvaluator, labelTransformationMethod, numLabelsToKeep);
                if (applySelection) {
                    Logger.getLogger(getClass()).info("APPLYING FEATURE SELECTION");
                    trainData = WekaUtils.applyAttributeSelectionFilter(trainData, attSel);
                    testData = WekaUtils.applyAttributeSelectionFilter(testData, attSel);
                }
            }
        }
        
        // build classifier
        Classifier cl = WekaUtils.getClassifier(learningMode, classificationArguments);

        // file to hold prediction results
        File evalOutput = WekaUtils.getFile(aContext, "", evaluationBin, AccessMode.READWRITE);

        // evaluation & prediction generation
        if (multiLabel) {
            // we don't need to build the classifier - meka does this
            // internally
            Result r = WekaUtils.getEvaluationMultilabel(cl, trainData, testData, threshold);
            WekaUtils.writeMlResultToFile(new MultilabelResult(r.allTrueValues(), r.allPredictions(),
                    threshold), evalOutput);
            testData = WekaUtils.getPredictionInstancesMultiLabel(testData, cl,
                    WekaUtils.getMekaThreshold(threshold, r, trainData));
            testData = WekaUtils.addInstanceId(testData, copyTestData, true);
        }
        else {
            // train the classifier on the train set split - not necessary in multilabel setup, but
            // in single label setup
            cl.buildClassifier(trainData);
            weka.core.SerializationHelper.write(evalOutput.getAbsolutePath(),
                    WekaUtils.getEvaluationSinglelabel(cl, trainData, testData));
            testData = WekaUtils.getPredictionInstancesSingleLabel(testData, cl);
            testData = WekaUtils.addInstanceId(testData, copyTestData, false);
        }

        // Write out the prediction - the data sink expects an .arff ending file so we game it a bit and rename the file afterwards to .txt
        File predictionFile = WekaUtils.getFile(aContext, "", Constants.FILENAME_PREDICTIONS, AccessMode.READWRITE);
        File arffDummy = new File(predictionFile.getParent(), "prediction.arff");
        DataSink.write(arffDummy.getAbsolutePath(), testData);
        FileUtils.moveFile(arffDummy, predictionFile);
    }

}