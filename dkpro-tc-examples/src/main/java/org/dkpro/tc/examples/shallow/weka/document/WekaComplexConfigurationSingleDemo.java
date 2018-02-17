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
package org.dkpro.tc.examples.shallow.weka.document;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.AvgTokenLengthRatioPerDocument;
import org.dkpro.tc.features.maxnormalization.AvgTokenRatioPerDocument;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.io.FolderwiseDataReader;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.weka.WekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.meta.Bagging;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;

/**
 * This demo is to show-case a somewhat more complex experiment setup for a single-label experiment,
 * including parameter sweeping (6 different combinations), (Weka) classifier configuration, and
 * Feature Selection.
 * 
 */
public class WekaComplexConfigurationSingleDemo
    implements Constants
{
    private static final String CORPUS_FILEPATH_TRAIN = "src/main/resources/data/twentynewsgroups/bydate-train";
    private static final String COPRUS_FILEPATH_TEST = "src/main/resources/data/twentynewsgroups/bydate-test";
    private static final String LANGUAGE_CODE = "en";
    private static final String EXPERIMENT_NAME = "TwentyNewsgroupsComplex";

    /**
     * Starts the experiment.
     */
    public static void main(String[] args)
        throws Exception
    {
        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(WekaComplexConfigurationSingleDemo.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();
        WekaComplexConfigurationSingleDemo experiment = new WekaComplexConfigurationSingleDemo();
        experiment.runTrainTest(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class,
                FolderwiseDataReader.PARAM_SOURCE_LOCATION, CORPUS_FILEPATH_TRAIN,
                FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class,
                FolderwiseDataReader.PARAM_SOURCE_LOCATION, COPRUS_FILEPATH_TEST,
                FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TEST, readerTest);

        // We configure 3 different classifiers, which will be swept, each with a special
        // configuration.
        Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                // "-C": complexity, "-K": kernel
                asList(new Object[] { new WekaAdapter(), SMO.class.getName(), "-C", "1.0", "-K",
                        PolyKernel.class.getName() + " " + "-C -1 -E 2" }),
                // "-I": number of trees
                asList(new Object[] { new WekaAdapter(), RandomForest.class.getName(), "-I", "5" }),
                // "W": base classifier
                asList(new Object[] { new WekaAdapter(), Bagging.class.getName(), "-I", "2", "-W", J48.class.getName(),
                        "--", "-C", "0.5", "-M", "2" }));

        // We configure 2 sets of feature extractors, one consisting of 3 extractors, and one with
        // only 1
        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(AvgTokenRatioPerDocument.class),
                        TcFeatureFactory.create(AvgTokenLengthRatioPerDocument.class),
                        TcFeatureFactory.create(WordNGram.class,
                                WordNGram.PARAM_NGRAM_USE_TOP_K, 50,
                                WordNGram.PARAM_NGRAM_MIN_N, 1, WordNGram.PARAM_NGRAM_MAX_N,
                                3)),
                new TcFeatureSet(TcFeatureFactory.create(WordNGram.class,
                        WordNGram.PARAM_NGRAM_USE_TOP_K, 50, WordNGram.PARAM_NGRAM_MIN_N, 1,
                        WordNGram.PARAM_NGRAM_MAX_N, 3)));

        // single-label feature selection (Weka specific options), reduces the feature set to 10
        Map<String, Object> dimFeatureSelection = new HashMap<String, Object>();
        dimFeatureSelection.put(DIM_FEATURE_SEARCHER_ARGS,
                asList(new String[] { Ranker.class.getName(), "-N", "10" }));
        dimFeatureSelection.put(DIM_ATTRIBUTE_EVALUATOR_ARGS,
                asList(new String[] { InfoGainAttributeEval.class.getName() }));
        dimFeatureSelection.put(DIM_APPLY_FEATURE_SELECTION, true);

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets,
                dimClassificationArgs,
                Dimension.createBundle("featureSelection", dimFeatureSelection));

        return pSpace;
    }

    // ##### TRAIN-TEST #####
    public void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentTrainTest batch = new ExperimentTrainTest(EXPERIMENT_NAME);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.addReport(BatchTrainTestReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {
        return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class,
                BreakIteratorSegmenter.PARAM_LANGUAGE, LANGUAGE_CODE));
    }
}
