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
package org.dkpro.tc.examples.multi.document;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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
import org.dkpro.tc.examples.io.anno.MultiLabelOutcomeAnnotator;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.length.NrOfTokens;
import org.dkpro.tc.features.ngram.LuceneNGram;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.weka.MekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import meka.classifiers.multilabel.BR;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.bayes.NaiveBayes;

public class MekaReutersDemoSimpleDkproReader
    implements Constants
{

    public static final String EXPERIMENT_NAME = "ReutersTextClassificationUsingTCEvaluation";
    public static final String FILEPATH_TRAIN = "src/main/resources/data/reuters/training";
    public static final String FILEPATH_TEST = "src/main/resources/data/reuters/test";
    public static final String FILEPATH_GOLD_LABELS = "src/main/resources/data/reuters/cats.txt";
    public static final String LANGUAGE_CODE = "en";
    public static final String BIPARTITION_THRESHOLD = "0.5";
    public static final int NUM_FOLDS = 3;

    public static void main(String[] args)
        throws Exception
    {
        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(MekaReutersDemoSimpleDkproReader.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();
        MekaReutersDemoSimpleDkproReader experiment = new MekaReutersDemoSimpleDkproReader();
        experiment.runTrainTest(pSpace);
//        experiment.runCrossValidation(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TextReader.class, TextReader.PARAM_SOURCE_LOCATION, FILEPATH_TRAIN,
                TextReader.PARAM_LANGUAGE, LANGUAGE_CODE, TextReader.PARAM_PATTERNS,
                TextReader.INCLUDE_PREFIX + "*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                TextReader.class, TextReader.PARAM_SOURCE_LOCATION, FILEPATH_TEST,
                TextReader.PARAM_LANGUAGE, LANGUAGE_CODE, TextReader.PARAM_PATTERNS,
                TextReader.INCLUDE_PREFIX + "*.txt");
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(
                        new Object[] { new MekaAdapter(), BR.class.getName(), "-W", NaiveBayes.class.getName() }));

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(
                DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(NrOfTokens.class), 
                                  TcFeatureFactory.create(LuceneNGram.class, 
                                                          LuceneNGram.PARAM_NGRAM_USE_TOP_K, "100",
                                                          LuceneNGram.PARAM_NGRAM_MIN_N, 1, 
                                                          LuceneNGram.PARAM_NGRAM_MAX_N, 3)));

        Map<String, Object> dimFeatureSelection = new HashMap<String, Object>();
        dimFeatureSelection.put(DIM_LABEL_TRANSFORMATION_METHOD,
                "BinaryRelevanceAttributeEvaluator");
        dimFeatureSelection.put(DIM_ATTRIBUTE_EVALUATOR_ARGS,
                Arrays.asList(new String[] { InfoGainAttributeEval.class.getName() }));
        dimFeatureSelection.put(DIM_NUM_LABELS_TO_KEEP, 10);
        dimFeatureSelection.put(DIM_APPLY_FEATURE_SELECTION, true);

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_MULTI_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT),
                Dimension.create(DIM_BIPARTITION_THRESHOLD, BIPARTITION_THRESHOLD),
                 dimFeatureSets, dimClassificationArgs,
                Dimension.createBundle("featureSelection", dimFeatureSelection));

        return pSpace;
    }

    // ##### TRAIN-TEST #####
    protected void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentTrainTest batch = new ExperimentTrainTest(EXPERIMENT_NAME + "-TrainTest");
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchTrainTestReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    // ##### CV #####
    protected void runCrossValidation(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentCrossValidation batch = new ExperimentCrossValidation(EXPERIMENT_NAME + "-CV", NUM_FOLDS);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchCrossValidationReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {

        return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class),
                createEngineDescription(MultiLabelOutcomeAnnotator.class,
                        MultiLabelOutcomeAnnotator.PARAM_GOLD_LABEL_FILE, FILEPATH_GOLD_LABELS),
                createEngineDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        LANGUAGE_CODE));
    }
}
