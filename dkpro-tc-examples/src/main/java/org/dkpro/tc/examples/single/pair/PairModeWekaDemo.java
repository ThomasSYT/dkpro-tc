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
package org.dkpro.tc.examples.single.pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.dkpro.tc.examples.io.PairTwentyNewsgroupsReader;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.pair.core.length.DiffNrOfTokensPairFeatureExtractor;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.weka.WekaAdapter;

import weka.classifiers.bayes.NaiveBayes;

/**
 * PairTwentyNewsgroupsExperiment, using Java
 * 
 * The PairTwentyNewsgroupsExperiment takes pairs of news files and trains/tests a binary classifier
 * to learn if the files in the pair are from the same newsgroup. The pairs are listed in a tsv
 * file: see the files in src/main/resources/lists/ as examples.
 * <p>
 * PairTwentyNewsgroupsExperiment uses similar architecture as TwentyNewsgroupsGroovyExperiment (
 * {@link ExperimentTrainTest}) to automatically wire the standard tasks for a basic TrainTest
 * setup. To remind the user to be careful of information leak when training and testing on pairs of
 * data from similar sources, we do not provide a demo Cross Validation setup here. (Our sample
 * train and test datasets are from separate newsgroups.) Please see
 * TwentyNewsgroupsGroovyExperiment for a demo implementing a CV experiment.
 */
public class PairModeWekaDemo
    implements Constants
{

    public static final String experimentName = "PairTwentyNewsgroupsExperiment";
    public static final String languageCode = "en";
    public static final String listFilePathTrain = "src/main/resources/data/twentynewsgroups/pairs/pairslist.train";
    public static final String listFilePathTest = "src/main/resources/data/twentynewsgroups/pairs/pairslist.test";

    public static void main(String[] args)
        throws Exception
    {
        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
        DemoUtils.setDkproHome(PairModeWekaDemo.class.getSimpleName());

        ParameterSpace pSpace = getParameterSpace();
        PairModeWekaDemo experiment = new PairModeWekaDemo();
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
                PairTwentyNewsgroupsReader.class, PairTwentyNewsgroupsReader.PARAM_LISTFILE,
                listFilePathTrain, PairTwentyNewsgroupsReader.PARAM_LANGUAGE_CODE, languageCode);
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
                PairTwentyNewsgroupsReader.class, PairTwentyNewsgroupsReader.PARAM_LISTFILE,
                listFilePathTest, PairTwentyNewsgroupsReader.PARAM_LANGUAGE_CODE, languageCode);
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new Object[] { new WekaAdapter(), NaiveBayes.class.getName() }));

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(
                DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(DiffNrOfTokensPairFeatureExtractor.class)));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_PAIR), dimFeatureSets,
                dimClassificationArgs);

        return pSpace;
    }

    // ##### TRAIN-TEST #####
    protected void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {

        ExperimentTrainTest batch = new ExperimentTrainTest("TwentyNewsgroupsTrainTest");
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);

        // Run
        Lab.getInstance().run(batch);
    }

}
