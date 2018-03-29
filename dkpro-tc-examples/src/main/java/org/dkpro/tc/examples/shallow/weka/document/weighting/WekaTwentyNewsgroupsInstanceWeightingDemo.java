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
package org.dkpro.tc.examples.shallow.weka.document.weighting;

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
import org.dkpro.tc.examples.shallow.io.WeightedTwentyNewsgroupsCorpusReader;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.TokenRatioPerDocument;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.weka.WekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import weka.classifiers.functions.SMO;

/**
 * This is the TwentyNewsgroups demo with instance weighting. With instance weighting, some
 * instances are considered by the machine learner/classifier to be more important than others. For
 * example, if some instances in your training set were annotated by a very reliable annotator, and
 * other instances were annotated by an unreliable worker, then the instances can be weighted more
 * and less heavily, respectively. <br>
 * 
 * An instance weight is a double value greater than 0. As can be seen in the demo below,
 * implementing instance weighting in DKPro TC consists of building a Reader that adds weights, and
 * setting the ParameterSpace dimension DIM_APPLY_INSTANCE_WEIGHTING to <b>true</b>.<br>
 * 
 * You may wish to weight just your training instances, and not your test instances, or vice versa.
 * You can control this in your Reader. Be careful when using cross-validation, if you only want
 * part of your dataset to be weighted. <br>
 * 
 * Currently, DKPro TC only supports instance weighting with Weka (see
 * {@link weka.core.WeightedInstancesHandler}); a full list of Weka classifiers that support
 * instance weighting can be found here: (@link
 * http://weka.sourceforge.net/doc.dev/weka/core/WeightedInstancesHandler.html}. This list includes
 * such common classifiers as J48, JRip, LinearReression, Logistic, MultilayerPerceptron,
 * NaiveBayes, SMO, SMOreg, and ZeroR. Users should familiarize themselves with the exact classifier
 * and weight utilization, as this varies per classifier. <br>
 * 
 * @see <a href="https://weka.wikispaces.com/Add+weights+to+dataset">Add weights to dataset</a>
 */
public class WekaTwentyNewsgroupsInstanceWeightingDemo
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static final String corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train";
    public static final String corpusFilePathTest = "src/main/resources/data/twentynewsgroups/bydate-test";
    public static final String weightsFile = "/arbitraryWeights.txt";

    public static void main(String[] args) throws Exception
    {

        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(WekaTwentyNewsgroupsInstanceWeightingDemo.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();

        WekaTwentyNewsgroupsInstanceWeightingDemo experiment = new WekaTwentyNewsgroupsInstanceWeightingDemo();

        experiment.runTrainTest(pSpace);
        experiment.runCrossValidation(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace() throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                WeightedTwentyNewsgroupsCorpusReader.class,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_WEIGHT_FILE_LOCATION,
                corpusFilePathTrain + weightsFile,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
                Arrays.asList(WeightedTwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"));
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                WeightedTwentyNewsgroupsCorpusReader.class,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTest,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_WEIGHT_FILE_LOCATION,
                corpusFilePathTest + weightsFile,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                WeightedTwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
                WeightedTwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt");
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new Object[] { new WekaAdapter(), SMO.class.getName() }));

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(
                        TcFeatureFactory.create(TokenRatioPerDocument.class),
                        TcFeatureFactory.create(WordNGram.class, WordNGram.PARAM_NGRAM_USE_TOP_K,
                                50, WordNGram.PARAM_NGRAM_MIN_N, 2, WordNGram.PARAM_NGRAM_MAX_N,
                                3)));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets,
                dimClassificationArgs, Dimension.create(DIM_APPLY_INSTANCE_WEIGHTING, true)
        // This last dimension is crucial for this demo
        );

        return pSpace;
    }

    // ##### TRAIN-TEST #####
    public void runTrainTest(ParameterSpace pSpace) throws Exception
    {

        ExperimentTrainTest experiment = new ExperimentTrainTest("TwentyNewsgroupsTrainTest");
        // each outcome label
        experiment.setPreprocessing(getPreprocessing());
        experiment.setParameterSpace(pSpace);
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        experiment.addReport(BatchTrainTestReport.class);

        // Run
        Lab.getInstance().run(experiment);
    }

    // ##### TRAIN-TEST #####
    protected void runCrossValidation(ParameterSpace pSpace) throws Exception
    {

        ExperimentCrossValidation experiment = new ExperimentCrossValidation(
                "TwentyNewsgroupsTrainTest", 2);
        // each outcome label
        experiment.setPreprocessing(getPreprocessing());
        experiment.setParameterSpace(pSpace);
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);

        // Run
        Lab.getInstance().run(experiment);
    }

    protected AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException
    {

        return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class));
    }
}
