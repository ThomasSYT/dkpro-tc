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
package org.dkpro.tc.examples.deeplearning.dl4j.sequence;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.shallow.annotators.SequenceOutcomeAnnotator;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.ml.builder.FeatureMode;
import org.dkpro.tc.ml.builder.LearningMode;
import org.dkpro.tc.ml.builder.MLBackend;
import org.dkpro.tc.ml.deeplearning4j.Deeplearning4jAdapter;
import org.dkpro.tc.ml.experiment.builder.DeepExperimentBuilder;
import org.dkpro.tc.ml.experiment.builder.ExperimentType;

import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;

/**
 * This a pure Java-based experiment setup of POS tagging as sequence tagging.
 */
public class DeepLearningDl4jSeq2SeqTrainTest
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei/keras";
    public static final String corpusFilePathTest = "src/main/resources/data/brown_tei/keras";

    public static void main(String[] args) throws Exception
    {

    	DemoUtils.setDkproHome(DeepLearningDl4jSeq2SeqTrainTest.class.getSimpleName());
    	
        DeepExperimentBuilder builder = new DeepExperimentBuilder();
        builder.experiment(ExperimentType.TRAIN_TEST, "dynetTrainTest")
               .dataReaderTrain(getTrainReader())
               .dataReaderTest(getTestReader())
               .learningMode(LearningMode.SINGLE_LABEL)
               .featureMode(FeatureMode.SEQUENCE)
               .preprocessing(getPreprocessing())
               .embeddingPath("src/test/resources/wordvector/glove.6B.50d_250.txt")
               .pythonPath("/usr/local/bin/python3")
               .maximumLength(100)
               .vectorizeToInteger(true)
               .machineLearningBackend(
                           new MLBackend(new Deeplearning4jAdapter(), new Dl4jSeq2SeqUserCode())
                       )
               .run();

//        ParameterSpace pSpace = getParameterSpace();
//
//        DeepLearningDl4jSeq2SeqTrainTest experiment = new DeepLearningDl4jSeq2SeqTrainTest();
//        experiment.runTrainTest(pSpace, null, null);
    }

    private static CollectionReaderDescription getTestReader() throws ResourceInitializationException
    {
        return CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTest, TeiReader.PARAM_PATTERNS, "*.xml");
    }

    private static CollectionReaderDescription getTrainReader() throws ResourceInitializationException
    {
        return CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTrain, TeiReader.PARAM_PATTERNS, "*.xml");
    }

//    public static ParameterSpace getParameterSpace() throws ResourceInitializationException
//    {
//        // configure training and test data reader dimension
//        Map<String, Object> dimReaders = new HashMap<String, Object>();
//
//        CollectionReaderDescription train = CollectionReaderFactory.createReaderDescription(
//                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
//                corpusFilePathTrain, TeiReader.PARAM_PATTERNS, "*.xml");
//        dimReaders.put(DIM_READER_TRAIN, train);
//
//        CollectionReaderDescription test = CollectionReaderFactory.createReaderDescription(
//                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
//                corpusFilePathTest, TeiReader.PARAM_PATTERNS, "*.xml");
//        dimReaders.put(DIM_READER_TEST, test);
//
//        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
//                Dimension.create(DIM_FEATURE_MODE, Constants.FM_SEQUENCE),
//                Dimension.create(DIM_LEARNING_MODE, Constants.LM_SINGLE_LABEL),
//                Dimension.create(DeepLearningConstants.DIM_PRETRAINED_EMBEDDINGS,
//                        "src/test/resources/wordvector/glove.6B.50d_250.txt"),
//                Dimension.create(DeepLearningConstants.DIM_VECTORIZE_TO_INTEGER, false),
//                Dimension.create(DeepLearningConstants.DIM_USE_ONLY_VOCABULARY_COVERED_BY_EMBEDDING,
//                        true),
//                Dimension.create(DeepLearningConstants.DIM_USER_CODE, new Dl4jSeq2SeqUserCode()));
//
//        return pSpace;
//    }

    protected static AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException
    {
        return createEngineDescription(SequenceOutcomeAnnotator.class);
    }

//    public void runTrainTest(ParameterSpace pSpace, ReportBase r, ContextMemoryReport r2)
//        throws Exception
//    {
//
//        DemoUtils.setDkproHome(DeepLearningDl4jSeq2SeqTrainTest.class.getSimpleName());
//
//        DeepLearningExperimentTrainTest experiment = new DeepLearningExperimentTrainTest(
//                "dl4jSeq2Seq", Deeplearning4jAdapter.class);
//        experiment.setParameterSpace(pSpace);
//        experiment.setPreprocessing(getPreprocessing());
//        if (r != null) {
//            experiment.addReport(r);
//        }
//        if (r2 != null) {
//            experiment.addReport(r2);
//        }
//        experiment.addReport(new TrainTestReport());
//        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
//
//        Lab.getInstance().run(experiment);
//    }
}
