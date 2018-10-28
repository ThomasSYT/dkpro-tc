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
package org.dkpro.tc.examples.shallow;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.shallow.annotators.UnitOutcomeAnnotator;
import org.dkpro.tc.examples.shallow.filter.UniformClassDistributionFilter;
import org.dkpro.tc.features.maxnormalization.TokenRatioPerDocument;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.ml.builder.FeatureMode;
import org.dkpro.tc.ml.builder.LearningMode;
import org.dkpro.tc.ml.builder.MLBackend;
import org.dkpro.tc.ml.experiment.builder.ExperimentBuilder;
import org.dkpro.tc.ml.experiment.builder.ExperimentType;
import org.dkpro.tc.ml.vowpalwabbit.VowpalWabbitAdapter;

import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;

/**
 * This is an example for POS tagging as unit classification. Each POS is treated as a
 * classification unit, but unlike sequence tagging the decision for each POS is taken
 * independently. This will usually give worse results, so this is only to showcase the concept.
 */
public class VowpalWabbitDemoUnit
    implements Constants
{

    public static final String LANGUAGE_CODE = "en";

    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei/";

    public static void main(String[] args) throws Exception
    {
    	System.setProperty("java.util.logging.config.file", "logging.properties");
    	System.setProperty("DKPRO_HOME", System.getProperty("user.home")+"/Desktop/");

        new VowpalWabbitDemoUnit().runTrainTest();
    }
    
    public CollectionReaderDescription getReaderTrain() throws Exception {
        return CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                TeiReader.PARAM_PATTERNS, "*.xml");
    }
    
    public CollectionReaderDescription getReaderTest() throws Exception {
        return CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                TeiReader.PARAM_PATTERNS, "*.xml");
    }
    
    public TcFeatureSet getFeatureSet() {
        return new TcFeatureSet(TcFeatureFactory.create(TokenRatioPerDocument.class),
        		TcFeatureFactory.create(WordNGram.class,
        				WordNGram.PARAM_NGRAM_USE_TOP_K, 500),
                TcFeatureFactory.create(CharacterNGram.class,
                        CharacterNGram.PARAM_NGRAM_USE_TOP_K, 500));
    }

    // ##### Train Test #####
    public void runTrainTest() throws Exception
    {

        ExperimentBuilder builder = new ExperimentBuilder();
        builder.experiment(ExperimentType.TRAIN_TEST, "trainTest")
                .dataReaderTrain(getReaderTrain())
                .dataReaderTest(getReaderTest())
                .preprocessing(getPreprocessing())
                .featureSets(getFeatureSet())
                .featureFilter(UniformClassDistributionFilter.class.getName())
                .learningMode(LearningMode.SINGLE_LABEL)
                .featureMode(FeatureMode.UNIT)
                .machineLearningBackend(
                        new MLBackend(new VowpalWabbitAdapter(), "--ect")
                        )
                .run();
    }
    

    protected AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException
    {
        return createEngineDescription(UnitOutcomeAnnotator.class);
    }
}