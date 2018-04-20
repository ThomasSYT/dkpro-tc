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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.shallow.misc.UnitOutcomeAnnotator;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.weka.WekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;
import weka.classifiers.bayes.NaiveBayes;

public class WekaManualFoldCrossValidation
    implements Constants
{
    public static final String LANGUAGE_CODE = "de";
    public static final int NUM_FOLDS = 2;
    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei/";

    public static void main(String[] args) throws Exception
    {
        WekaManualFoldCrossValidation demo = new WekaManualFoldCrossValidation();
        demo.runCrossValidation(getParameterSpace(true), NUM_FOLDS);
    }

    // ##### CV #####
    public void runCrossValidation(ParameterSpace pSpace, int folds) throws Exception
    {
        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(WekaManualFoldCrossValidation.class.getSimpleName());

        ExperimentCrossValidation experiment = new ExperimentCrossValidation("NERDemoCV", folds);
        experiment.setParameterSpace(pSpace);
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        experiment.addReport(new BatchCrossValidationReport());
        experiment.setPreprocessing(AnalysisEngineFactory.createEngineDescription(UnitOutcomeAnnotator.class));

        // Run
        Lab.getInstance().run(experiment);
    }

    public static ParameterSpace getParameterSpace(boolean manualFolds)
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "de",
                TeiReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                TeiReader.PARAM_PATTERNS, "*.xml");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(CharacterNGram.class,
                        CharacterNGram.PARAM_NGRAM_MIN_N, 2, CharacterNGram.PARAM_NGRAM_MAX_N, 3,
                        CharacterNGram.PARAM_NGRAM_USE_TOP_K, 750)));

        Map<String, Object> config = new HashMap<>();
        config.put(DIM_CLASSIFICATION_ARGS,
                new Object[] { new WekaAdapter(), NaiveBayes.class.getName() });
        config.put(DIM_DATA_WRITER, new WekaAdapter().getDataWriterClass());
        config.put(DIM_FEATURE_USE_SPARSE, new WekaAdapter().useSparseFeatures());

        Dimension<Map<String, Object>> mlas = Dimension.createBundle("config", config);

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_UNIT), dimFeatureSets, mlas,
                /*
                 * MANUAL CROSS VALIDATION FOLDS - i.e. the cas created by your reader will be used
                 * as is to make folds
                 */
                Dimension.create(DIM_CROSS_VALIDATION_MANUAL_FOLDS, manualFolds));

        return pSpace;
    }

}
