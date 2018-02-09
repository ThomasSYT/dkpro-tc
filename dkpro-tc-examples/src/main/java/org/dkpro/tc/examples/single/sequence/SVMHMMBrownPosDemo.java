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
package org.dkpro.tc.examples.single.sequence;

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.ml.TcShallowLearningAdapter;
import org.dkpro.tc.examples.io.BrownCorpusReader;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.length.NrOfChars;
import org.dkpro.tc.features.ngram.LuceneCharacterNGram;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.svmhmm.SvmHmmAdapter;

/**
 * Tests SVMhmm on POS tagging of one file in Brown corpus
 */
public class SVMHMMBrownPosDemo
{

    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei";
    private static final int NUM_FOLDS = 3;

    public static Map<String, Object> getDimReaders(boolean trainTest)
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> results = new HashMap<>();

        if (trainTest) {

            CollectionReaderDescription readerTrain = CollectionReaderFactory
                    .createReaderDescription(BrownCorpusReader.class,
                            BrownCorpusReader.PARAM_LANGUAGE, "en",
                            BrownCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                            BrownCorpusReader.PARAM_PATTERNS, "a01.xml");

            CollectionReaderDescription readerTest = CollectionReaderFactory
                    .createReaderDescription(BrownCorpusReader.class,
                            BrownCorpusReader.PARAM_LANGUAGE, "en",
                            BrownCorpusReader.PARAM_LANGUAGE, "en",
                            BrownCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                            BrownCorpusReader.PARAM_PATTERNS, "a02.xml");

            results.put(Constants.DIM_READER_TRAIN, readerTrain);
            results.put(Constants.DIM_READER_TEST, readerTest);
        }
        else {
            CollectionReaderDescription readerTrain = CollectionReaderFactory
                    .createReaderDescription(BrownCorpusReader.class,
                            BrownCorpusReader.PARAM_LANGUAGE, "en",
                            BrownCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                            BrownCorpusReader.PARAM_PATTERNS,
                            Arrays.asList(INCLUDE_PREFIX + "*.xml"));

            results.put(Constants.DIM_READER_TRAIN, readerTrain);
        }

        return results;
    }

    public static ParameterSpace getParameterSpace(boolean trainTest,
            Dimension<List<Object>> dimClassificationArgs)
                throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> dimReaders = getDimReaders(trainTest);

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(
                Constants.DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(NrOfChars.class),
                        TcFeatureFactory.create(LuceneCharacterNGram.class,
                                LuceneCharacterNGram.PARAM_NGRAM_USE_TOP_K, 20,
                                LuceneCharacterNGram.PARAM_NGRAM_MIN_N, 2,
                                LuceneCharacterNGram.PARAM_NGRAM_MAX_N, 3)));

        return new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(Constants.DIM_LEARNING_MODE, Constants.LM_SINGLE_LABEL),
                Dimension.create(Constants.DIM_FEATURE_MODE, Constants.FM_SEQUENCE),
                dimFeatureSets, dimClassificationArgs);
    }

    protected void runCrossValidation(ParameterSpace pSpace,
            Class<? extends TcShallowLearningAdapter> machineLearningAdapter)
                throws Exception
    {
        final ExperimentCrossValidation batch = new ExperimentCrossValidation("BrownCVBatchTask", NUM_FOLDS);
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchCrossValidationReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected void runTrainTest(ParameterSpace pSpace)
                throws Exception
    {
        final ExperimentTrainTest batch = new ExperimentTrainTest("BrownTrainTestBatchTask");
        batch.setParameterSpace(pSpace);
        batch.addReport(BatchTrainTestReport.class);
        batch.addReport(ContextMemoryReport.class);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);

        // Run
        Lab.getInstance().run(batch);
    }

    public static void main(String[] args)
        throws Exception
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimClassificationArgs = Dimension
                .create(Constants.DIM_CLASSIFICATION_ARGS, Arrays.asList(new SvmHmmAdapter()));

        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(SVMHMMBrownPosDemo.class.getSimpleName());

        // run cross-validation first
        ParameterSpace pSpace = getParameterSpace(false, dimClassificationArgs);

        SVMHMMBrownPosDemo experiment = new SVMHMMBrownPosDemo();
        // run with a random labeler
        // experiment.runCrossValidation(pSpace, RandomSVMHMMAdapter.class);
        // run with an actual SVMHMM implementation
        // experiment.runCrossValidation(pSpace, SVMHMMAdapter.class);
        //
        // // run train test
        pSpace = getParameterSpace(true, dimClassificationArgs);
        //
        // experiment = new SVMHMMBrownPosDemo();
        // // run with a random labeler
        // experiment.runTrainTest(pSpace, RandomSVMHMMAdapter.class);
        // // run with an actual SVMHMM implementation
        experiment.runTrainTest(pSpace);
    }

}
