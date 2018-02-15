/*******************************************************************************
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.tc.features.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.Instance;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.io.JsonDataWriter;
import org.dkpro.tc.core.util.TaskUtils;
import org.dkpro.tc.features.ngram.io.TestReaderSingleLabel;
import org.dkpro.tc.features.ngram.meta.WordNGramMC;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class NGramFeatureExtractorTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setupLogging()
    {
        System.setProperty("org.apache.uima.logger.class",
                "org.apache.uima.util.impl.Log4jLogger_impl");
    }

    @Test
    public void luceneNGramFeatureExtractorTest()
        throws Exception
    {

        File luceneFolder = folder.newFolder();
        File outputPath = folder.newFolder();

        Object[] parameters = new Object[] { WordNGram.PARAM_UNIQUE_EXTRACTOR_NAME, "123",
                WordNGram.PARAM_NGRAM_USE_TOP_K, "3", WordNGram.PARAM_SOURCE_LOCATION,
                luceneFolder.toString(), WordNGramMC.PARAM_TARGET_LOCATION,
                luceneFolder.toString() };

        ExternalResourceDescription featureExtractor = ExternalResourceFactory
                .createExternalResourceDescription(WordNGram.class, parameters);
        List<ExternalResourceDescription> fes = new ArrayList<>();
        fes.add(featureExtractor);

        List<Object> parameterList = new ArrayList<Object>(Arrays.asList(parameters));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderSingleLabel.class, TestReaderSingleLabel.PARAM_SOURCE_LOCATION,
                "src/test/resources/ngrams/*.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription metaCollector = AnalysisEngineFactory
                .createEngineDescription(WordNGramMC.class, parameterList.toArray());

        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_SINGLE_LABEL, Constants.FM_DOCUMENT, false, false, false, false,
                Collections.emptyList(), fes, new String[]{});

        // run meta collector
        SimplePipeline.runPipeline(reader, segmenter, metaCollector);

        // run FE(s)
        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        List<String> lines = FileUtils
                .readLines(new File(outputPath, JsonDataWriter.JSON_FILE_NAME), "utf-8");
        List<Instance> instances = new ArrayList<>();
        for (String l : lines) {
            instances.add(gson.fromJson(l, Instance.class));
        }
        
        assertEquals(4, instances.size());
        assertEquals(1, getUniqueOutcomes(instances));

        Set<String> featureNames = new HashSet<String>();
        for(Instance i : instances){
            for(Feature f : i.getFeatures()){
                featureNames.add(f.getName());
            }
        }
        assertEquals(3, featureNames.size());
        assertTrue(featureNames.contains("ngram_4"));
        assertTrue(featureNames.contains("ngram_5"));
        assertTrue(featureNames.contains("ngram_5_5"));

    }

    @Test
    public void luceneNGramFeatureExtractorNonDefaultFrequencyThresholdTest()
        throws Exception
    {

        File luceneFolder = folder.newFolder();
        File outputPath = folder.newFolder();

        Object[] parameters = new Object[] { WordNGram.PARAM_NGRAM_USE_TOP_K, "3",
                WordNGram.PARAM_UNIQUE_EXTRACTOR_NAME, "123", WordNGram.PARAM_SOURCE_LOCATION,
                luceneFolder.toString(), WordNGram.PARAM_NGRAM_FREQ_THRESHOLD, "0.1f",
                WordNGramMC.PARAM_TARGET_LOCATION, luceneFolder.toString() };

        List<Object> parameterList = new ArrayList<Object>(Arrays.asList(parameters));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderSingleLabel.class, TestReaderSingleLabel.PARAM_SOURCE_LOCATION,
                "src/test/resources/ngrams/*.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription metaCollector = AnalysisEngineFactory
                .createEngineDescription(WordNGramMC.class, parameterList.toArray());

        ExternalResourceDescription featureExtractor = ExternalResourceFactory
                .createExternalResourceDescription(WordNGram.class, parameters);
        List<ExternalResourceDescription> fes = new ArrayList<>();
        fes.add(featureExtractor);

        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_SINGLE_LABEL, Constants.FM_DOCUMENT, false, false, false, false,
                Collections.emptyList(), fes, new String[]{});

        // run meta collector
        SimplePipeline.runPipeline(reader, segmenter, metaCollector);

        // run FE(s)
        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        List<String> lines = FileUtils
                .readLines(new File(outputPath, JsonDataWriter.JSON_FILE_NAME),"utf-8");
        List<Instance> instances = new ArrayList<>();
        for (String l : lines) {
            instances.add(gson.fromJson(l, Instance.class));
        }

        assertEquals(4, instances.size());
        assertEquals(1, getUniqueOutcomes(instances));
        for (Instance i : instances) {
            assertTrue(i.getFeatures().isEmpty());
        }
    }

    private int getUniqueOutcomes(List<Instance> instances)
    {
        Set<String> outcomes = new HashSet<String>();
        instances.forEach(x -> outcomes.addAll(x.getOutcomes()));
        return outcomes.size();
    }
}
