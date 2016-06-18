/**
 * Copyright 2016
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
package org.dkpro.tc.examples.model;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.io.TwentyNewsgroupsCorpusReader;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.length.NrOfTokens;
import org.dkpro.tc.features.ngram.LuceneNGram;
import org.dkpro.tc.features.ngram.base.NGramFeatureExtractorBase;
import org.dkpro.tc.ml.ExperimentSaveModel;
import org.dkpro.tc.ml.liblinear.LiblinearAdapter;
import org.dkpro.tc.ml.liblinear.LiblinearTestTask;
import org.dkpro.tc.ml.uima.TcAnnotator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/**
 * Round-trip tests for save/load model experiments. Tests all feature modes (document, pair, unit),
 * as well as all learning models (single-label, multi-label, regression).
 *
 */
public class LiblinearSaveAndLoadModelTest
    implements Constants
{
    static String documentTrainFolder = "src/main/resources/data/twitter/train";
    static String documentTestFolder = "src/main/resources/data/twitter/test";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup()
    {
        DemoUtils.setDkproHome(LiblinearSaveAndLoadModelTest.class.getSimpleName());
    }

    private ParameterSpace documentGetParameterSpaceSingleLabel(boolean useClassificationArguments)
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TwentyNewsgroupsCorpusReader.class,
                TwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION, documentTrainFolder,
                TwentyNewsgroupsCorpusReader.PARAM_LANGUAGE, "en",
                TwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
                Arrays.asList(TwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"));
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimPipelineParameters = Dimension.create(DIM_PIPELINE_PARAMS,
                Arrays.asList(new Object[] { NGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K, 500,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MIN_N, 1,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MAX_N, 3 }));

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimClassificationArguments = Dimension.create(
                DIM_CLASSIFICATION_ARGS, Arrays.asList("-s", LiblinearTestTask.SOLVER_L1R_LR));

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimFeatureSets = Dimension.create(DIM_FEATURE_SET, Arrays
                .asList(new String[] { NrOfTokens.class.getName(), LuceneNGram.class.getName() }));

        ParameterSpace pSpace;
        if (useClassificationArguments) {
            pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                    Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                    Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimPipelineParameters,
                    dimClassificationArguments, dimFeatureSets);
        }
        else {
            pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                    Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                    Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimPipelineParameters,
                    dimFeatureSets);
        }
        return pSpace;
    }

    @Test
    public void roundTripTest()
        throws Exception
    {

        DemoUtils.setDkproHome(LiblinearSaveAndLoadModelTest.class.getSimpleName());
        File modelFolder = folder.newFolder();

        ParameterSpace docParamSpace = documentGetParameterSpaceSingleLabel(false);
        documentTrainAndStoreModel(docParamSpace, modelFolder);
        documentLoadAndUseModel(modelFolder, false);
        documentVerifyCreatedModelFiles(modelFolder);
        
        docParamSpace = documentGetParameterSpaceSingleLabel(true);
        documentTrainAndStoreModel(docParamSpace, modelFolder);
        documentLoadAndUseModel(modelFolder, true);

        

        modelFolder.deleteOnExit();
    }

    private void documentVerifyCreatedModelFiles(File modelFolder)
    {
        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File usedFeaturesFile = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_EXTRACTORS);
        assertTrue(usedFeaturesFile.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File id2outcomeMapping = new File(
                modelFolder.getAbsolutePath() + "/" + LiblinearAdapter.getOutcomeMappingFilename());
        assertTrue(id2outcomeMapping.exists());
    }

    private static void documentTrainAndStoreModel(ParameterSpace paramSpace, File modelFolder)
        throws Exception
    {
        ExperimentSaveModel batch = new ExperimentSaveModel("TestSaveModel", LiblinearAdapter.class,
                modelFolder);
        batch.setPreprocessing(
                createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class)));
        batch.setParameterSpace(paramSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(batch);
    }

    private static void documentLoadAndUseModel(File modelFolder,
            boolean evaluateWithClassificationArgs)
                throws Exception
    {
        AnalysisEngine tokenizer = AnalysisEngineFactory.createEngine(BreakIteratorSegmenter.class);

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath());

        CollectionReader reader = CollectionReaderFactory.createReader(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, documentTestFolder, TextReader.PARAM_LANGUAGE,
                "en", TextReader.PARAM_PATTERNS,
                Arrays.asList(TextReader.INCLUDE_PREFIX + "*/*.txt"));

        List<TextClassificationOutcome> outcomes = new ArrayList<>();
        while (reader.hasNext()) {
            JCas jcas = JCasFactory.createJCas();
            reader.getNext(jcas.getCas());
            jcas.setDocumentLanguage("en");

            tokenizer.process(jcas);
            tcAnno.process(jcas);

            outcomes.add(JCasUtil.selectSingle(jcas, TextClassificationOutcome.class));
        }

        assertEquals(4, outcomes.size());

        if (evaluateWithClassificationArgs) {
            assertEquals(4, outcomes.size());
            assertEquals("neutral", outcomes.get(0).getOutcome());
            assertEquals("neutral", outcomes.get(1).getOutcome());
            assertEquals("neutral", outcomes.get(2).getOutcome());
            assertEquals("neutral", outcomes.get(3).getOutcome());
        }
        else {
            assertEquals(4, outcomes.size());
            assertEquals("neutral", outcomes.get(0).getOutcome());
            assertEquals("emotional", outcomes.get(1).getOutcome());
            assertEquals("emotional", outcomes.get(2).getOutcome());
            assertEquals("emotional", outcomes.get(3).getOutcome());          
        }
    }

}