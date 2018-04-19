package org.dkpro.tc.examples;
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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.TokenRatioPerDocument;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.io.FolderwiseDataReader;
import org.dkpro.tc.ml.builder.FeatureMode;
import org.dkpro.tc.ml.builder.LearningMode;
import org.dkpro.tc.ml.weka.WekaAdapter;
import org.dkpro.tc.simple.builder.TcCrossValidationExperiment;
import org.dkpro.tc.simple.builder.TcTrainTestExperiment;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class MinimalWorkingExample
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static final String corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train";
    public static final String corpusFilePathTest = "src/main/resources/data/twentynewsgroups/bydate-test";

    public static void main(String[] args) throws Exception
    {
        DemoUtils.setDkproHome("target/");
        runExperiment();
    }

    public static void runExperiment() throws Exception
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the
        // train part
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class, FolderwiseDataReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTrain, FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class, FolderwiseDataReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTest, FolderwiseDataReader.PARAM_LANGUAGE, LANGUAGE_CODE,
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TEST, readerTest);

        TcFeatureSet tcFeatureSet = new TcFeatureSet("DummyFeatureSet",

                // The number of tokens in a document with respect to the document in the training
                // documents that has the most tokens
                TcFeatureFactory.create(TokenRatioPerDocument.class),

                // Word ngrams as features, we use the K most frequent ngrams in the range of uni,
                // bi and tri grams
                TcFeatureFactory.create(WordNGram.class, WordNGram.PARAM_NGRAM_USE_TOP_K, 20,
                        WordNGram.PARAM_NGRAM_MIN_N, 1, WordNGram.PARAM_NGRAM_MAX_N, 3));

        // ExperimentBuilder builder = new ExperimentBuilder(ExperimentType.TRAIN_TEST, readerTrain,
        // readerTest, LearningMode.SINGLE_LABEL, FeatureMode.DOCUMENT, new WekaAdapter(),
        // tcFeatureSet);
        // builder.setExperimentPreprocessing(createEngineDescription(BreakIteratorSegmenter.class));
        // builder.setExperimentName("ExampleProject");
        // builder.runExperiment();

        // ExperimentBuilderV2 builder = new ExperimentBuilderV2();
        // builder.setExperiment(ExperimentType.TRAIN_TEST, "trainTestExperiment", 2)
        // .setReader(readerTrain, true)
        // .setReader(readerTest, false)
        // .setMachineLearningBackend(new MLBackend(new WekaAdapter(), SMO.class.getName()),
        // new MLBackend(new LibsvmAdapter(), "-s", "1", "-c", "1000"))
        // .setExperimentPreprocessing(createEngineDescription(BreakIteratorSegmenter.class))
        // .setFeatureMode(FeatureMode.DOCUMENT)
        // .setLearningMode(LearningMode.SINGLE_LABEL)
        // .setFeatures(TcFeatureFactory.create(TokenRatioPerDocument.class),
        // TcFeatureFactory.create(WordNGram.class,
        // WordNGram.PARAM_NGRAM_USE_TOP_K, 20,
        // WordNGram.PARAM_NGRAM_MIN_N, 1,
        // WordNGram.PARAM_NGRAM_MAX_N, 3)
        // )
        // .run();

        TcTrainTestExperiment tte = new TcTrainTestExperiment(readerTrain, readerTest,
                LearningMode.SINGLE_LABEL, FeatureMode.DOCUMENT, new WekaAdapter(), tcFeatureSet,
                AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class));
        tte.run();
        
        TcCrossValidationExperiment cve = new TcCrossValidationExperiment(2, readerTrain,
                LearningMode.SINGLE_LABEL, FeatureMode.DOCUMENT, new WekaAdapter(), tcFeatureSet,
                AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class));
        cve.run();
    }
}
