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
package org.dkpro.tc.examples.shallow.libsvm.unit;

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX;
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
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.shallow.io.anno.UnitOutcomeAnnotator;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.features.ngram.NrOfTokensRatioDocument;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.libsvm.LibsvmAdapter;

import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;

/**
 * This is an example for POS tagging as unit classification. Each POS is treated as a
 * classification unit, but unlike sequence tagging the decision for each POS is taken
 * independently. This will usually give worse results, so this is only to showcase the concept.
 * 
 */
public class LibsvmBrownUnitPosDemo
    implements Constants
{

    public static final String LANGUAGE_CODE = "en";

    public static final int NUM_FOLDS = 2;

    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei/";

    public static void main(String[] args)
        throws Exception
    {
        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(LibsvmBrownUnitPosDemo.class.getSimpleName());

        new LibsvmBrownUnitPosDemo().runTrainTest(getParameterSpace());
        new LibsvmBrownUnitPosDemo().runCrossValidation(getParameterSpace());
    }

    // ##### CV #####
    public void runCrossValidation(ParameterSpace pSpace)
        throws Exception
    {

        ExperimentCrossValidation batch = new ExperimentCrossValidation(
                "LibsvmCrossvalidationBrownPosDemo", NUM_FOLDS);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(ContextMemoryReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    // ##### Train Test #####
    public void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {

        ExperimentTrainTest batch = new ExperimentTrainTest("LibsvmTrainTestBrownPosDemo");
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(ContextMemoryReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                TeiReader.PARAM_PATTERNS,
                new String[] { INCLUDE_PREFIX + "*.xml", INCLUDE_PREFIX + "*.xml.gz" });
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                TeiReader.PARAM_PATTERNS,
                new String[] { INCLUDE_PREFIX + "*.xml", INCLUDE_PREFIX + "*.xml.gz" });
        dimReaders.put(DIM_READER_TEST, readerTest);

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimClassificationArgs = Dimension
                .create(Constants.DIM_CLASSIFICATION_ARGS, asList(new Object[] { new LibsvmAdapter(), "-c", "10" }));

		Dimension<TcFeatureSet> dimFeatureSets = Dimension
				.create(Constants.DIM_FEATURE_SET,
						new TcFeatureSet(TcFeatureFactory.create(NrOfTokensRatioDocument.class),
								TcFeatureFactory.create(CharacterNGram.class,
										CharacterNGram.PARAM_NGRAM_LOWER_CASE, false,
										CharacterNGram.PARAM_NGRAM_USE_TOP_K, 50)));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_UNIT), dimFeatureSets, dimClassificationArgs);

        return pSpace;
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {
        return createEngineDescription(UnitOutcomeAnnotator.class);
    }
}
