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
package org.dkpro.tc.examples.deeplearning.keras.regression;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.DeepLearningConstants;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.io.DelimiterSeparatedValuesReader;
import org.dkpro.tc.ml.experiment.deep.DeepLearningExperimentCrossValidation;
import org.dkpro.tc.ml.keras.KerasAdapter;
import org.dkpro.tc.ml.report.CrossValidationReport;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class KerasRegression
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static void main(String[] args) throws Exception
    {

         DemoUtils.setDkproHome(KerasRegression.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace("/usr/local/bin/python3");

        KerasRegression.runCrossValidation(pSpace, new ContextMemoryReport());
    }

    public static ParameterSpace getParameterSpace(String pythonPath)
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                DelimiterSeparatedValuesReader.class, DelimiterSeparatedValuesReader.PARAM_OUTCOME_INDEX, 0,
                DelimiterSeparatedValuesReader.PARAM_TEXT_INDEX, 1,
                DelimiterSeparatedValuesReader.PARAM_SOURCE_LOCATION,
                "src/main/resources/data/essays/train/essay_train.txt",
                DelimiterSeparatedValuesReader.PARAM_LANGUAGE, "en");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                DelimiterSeparatedValuesReader.class, DelimiterSeparatedValuesReader.PARAM_OUTCOME_INDEX, 0,
                DelimiterSeparatedValuesReader.PARAM_TEXT_INDEX, 1,
                DelimiterSeparatedValuesReader.PARAM_SOURCE_LOCATION,
                "src/main/resources/data/essays/train/essay_test.txt",
                DelimiterSeparatedValuesReader.PARAM_LANGUAGE, "en");
        dimReaders.put(DIM_READER_TEST, readerTest);

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_FEATURE_MODE, Constants.FM_DOCUMENT),
                Dimension.create(DIM_LEARNING_MODE, Constants.LM_REGRESSION),
                Dimension.create(DeepLearningConstants.DIM_PYTHON_INSTALLATION, pythonPath),
                Dimension.create(DeepLearningConstants.DIM_USER_CODE,
                        "src/main/resources/kerasCode/regression/essay.py"),
                Dimension.create(DeepLearningConstants.DIM_MAXIMUM_LENGTH, 100),
                Dimension.create(DeepLearningConstants.DIM_VECTORIZE_TO_INTEGER, true),
                Dimension.create(DeepLearningConstants.DIM_PRETRAINED_EMBEDDINGS,
                        "src/test/resources/wordvector/glove.6B.50d_250.txt"));

        return pSpace;
    }

    public static void runCrossValidation(ParameterSpace pSpace, ContextMemoryReport contextReport) throws Exception
    {

        DeepLearningExperimentCrossValidation experiment = new DeepLearningExperimentCrossValidation(
                "KerasRegression", KerasAdapter.class, 2);
        experiment.setPreprocessing(getPreprocessing());
        experiment.setParameterSpace(pSpace);
        experiment.addReport(contextReport);
        experiment.addReport(CrossValidationReport.class);
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);

        // Run
        Lab.getInstance().run(experiment);
    }

    protected static AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {
        return createEngineDescription(BreakIteratorSegmenter.class);
    }
}
