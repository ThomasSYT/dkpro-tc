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
package org.dkpro.tc.examples.shallow.weka.document.filter;

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
import org.dkpro.tc.core.feature.filter.UniformClassDistributionFilter;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.ngram.NumberOfTokensRatio;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.io.FolderwiseDataReader;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.weka.WekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import weka.classifiers.bayes.NaiveBayes;

/**
 * This a pure Java-based experiment setup of the TwentyNewsgroupsExperiment.
 * 
 * Defining the parameters directly in this class makes on-the-fly changes more difficult when the
 * experiment is run on a server.
 * 
 * For these cases, the self-sufficient Groovy versions are more suitable, since their source code
 * can be changed and then executed without pre-compilation.
 */
public class WekaUniformClassDistributionDemo
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static final int NUM_FOLDS = 3;

    public static final String corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train";
    public static final String corpusFilePathTest = "src/main/resources/data/twentynewsgroups/bydate-test";

    public static void main(String[] args)
        throws Exception
    {

        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(WekaUniformClassDistributionDemo.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();

        WekaUniformClassDistributionDemo experiment = new WekaUniformClassDistributionDemo();
        // experiment.runCrossValidation(pSpace);
        experiment.runTrainTest(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class,
                FolderwiseDataReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class,
                FolderwiseDataReader.PARAM_SOURCE_LOCATION, corpusFilePathTest,
                FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new Object[] { new WekaAdapter(), NaiveBayes.class.getName() }));

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(NumberOfTokensRatio.class), TcFeatureFactory.create(
                        WordNGram.class, WordNGram.PARAM_NGRAM_USE_TOP_K, 50,
                        WordNGram.PARAM_NGRAM_MIN_N, 1, WordNGram.PARAM_NGRAM_MAX_N, 3)));

        Dimension<List<String>> dimFeatureFilters = Dimension.create(DIM_FEATURE_FILTERS,
                Arrays.asList(new String[] { UniformClassDistributionFilter.class.getName() }));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets, dimFeatureFilters,
                dimClassificationArgs);

        return pSpace;
    }

    // ##### CV #####
    public void runCrossValidation(ParameterSpace pSpace)
        throws Exception
    {

        ExperimentCrossValidation batch = new ExperimentCrossValidation(
                "UniformClassDistFeatureFilterCV", NUM_FOLDS);
        batch.setPreprocessing(getPreprocessing());
        // batch.addInnerReport(WekaClassificationReport.class);
        // add a second report to TestTask which creates a report about average feature values for
        // each outcome label
        // batch.addInnerReport(WekaFeatureValuesReport.class);
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchCrossValidationReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    // ##### TRAIN-TEST #####
    public void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {

        ExperimentTrainTest batch = new ExperimentTrainTest("UniformClassDistFeatureFilterTT");
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);

        // Run
        Lab.getInstance().run(batch);
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {

        return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class),
                createEngineDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        LANGUAGE_CODE));
    }
}
