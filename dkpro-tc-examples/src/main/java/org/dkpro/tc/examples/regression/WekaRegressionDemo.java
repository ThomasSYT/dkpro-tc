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
package org.dkpro.tc.examples.regression;

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
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.io.EssayScoreReader;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.length.NrOfSentences;
import org.dkpro.tc.features.length.NrOfTokens;
import org.dkpro.tc.features.length.NrOfTokensPerSentence;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.weka.WekaRegressionAdapter;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import weka.classifiers.functions.LinearRegression;

public class WekaRegressionDemo
    implements Constants
{

    public static void main(String[] args)
        throws Exception
    {

        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(WekaRegressionDemo.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();

        WekaRegressionDemo experiment = new WekaRegressionDemo();
        experiment.runTrainTest(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        // The reader is also responsible for setting the labels/outcome on all
        // documents/instances it creates.
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                EssayScoreReader.class, EssayScoreReader.PARAM_SOURCE_LOCATION,
                "src/main/resources/data/essays/train/essay_train.txt", EssayScoreReader.PARAM_LANGUAGE, "en"
                );
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                EssayScoreReader.class, EssayScoreReader.PARAM_SOURCE_LOCATION,
                "src/main/resources/data/essays/test/essay_test.txt", EssayScoreReader.PARAM_LANGUAGE, "en");
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<String>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { LinearRegression.class.getName() }));

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(
                DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(NrOfTokens.class),
                        TcFeatureFactory.create(NrOfSentences.class),
                        TcFeatureFactory.create(NrOfTokensPerSentence.class)));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_REGRESSION),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets,
                dimClassificationArgs);

        return pSpace;
    }


    // ##### TRAIN-TEST #####
    protected void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentTrainTest batch = new ExperimentTrainTest("WekaRegressionDemo",
                WekaRegressionAdapter.class);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.addReport(BatchTrainTestReport.class);
        batch.addReport(ContextMemoryReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {
        return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class));
    }
}
