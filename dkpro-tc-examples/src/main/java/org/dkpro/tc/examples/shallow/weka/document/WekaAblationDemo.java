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
import org.dkpro.tc.core.util.ExperimentUtil;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.AvgTokenRatioPerDocument;
import org.dkpro.tc.features.twitter.EmoticonRatio;
import org.dkpro.tc.features.twitter.NumberOfHashTags;
import org.dkpro.tc.io.FolderwiseDataReader;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.report.BatchRuntimeReport;
import org.dkpro.tc.ml.weka.WekaAdapter;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import weka.classifiers.bayes.NaiveBayes;

/**
 * Shows how to use the ablation test feature sets.
 */
public class WekaAblationDemo implements Constants {
	public static final String LANGUAGE_CODE = "en";

	public static final int NUM_FOLDS = 2;

	public static final String corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train";
	public static final String corpusFilePathTest = "src/main/resources/data/twentynewsgroups/bydate-test";

	public static void main(String[] args) throws Exception {

		// This is used to ensure that the required DKPRO_HOME environment
		// variable is set.
		// Ensures that people can run the experiments even if they haven't read
		// the setup
		// instructions first :)
		// Don't use this in real experiments! Read the documentation and set
		// DKPRO_HOME as
		// explained there.
		DemoUtils.setDkproHome(WekaAblationDemo.class.getSimpleName());

		ParameterSpace pSpace = getParameterSpace();

		WekaAblationDemo experiment = new WekaAblationDemo();
		experiment.runCrossValidation(pSpace);
	}

	@SuppressWarnings("unchecked")
	public static ParameterSpace getParameterSpace() throws ResourceInitializationException {
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

		Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
				Arrays.asList(new Object[] { new WekaAdapter(), NaiveBayes.class.getName() }));

		Dimension<TcFeatureSet> dimFeatureSets = ExperimentUtil.getAblationTestFeatures(
				TcFeatureFactory.create(AvgTokenRatioPerDocument.class), TcFeatureFactory.create(EmoticonRatio.class),
				TcFeatureFactory.create(NumberOfHashTags.class));

		ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
				Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL), Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT),
				dimFeatureSets, dimClassificationArgs);

		return pSpace;
	}

	// ##### CV #####
	public void runCrossValidation(ParameterSpace pSpace) throws Exception {

		ExperimentCrossValidation experiment = new ExperimentCrossValidation("TwentyNewsgroupsCV", NUM_FOLDS);
		experiment.setPreprocessing(getPreprocessing());
		experiment.setParameterSpace(pSpace);
		experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
		experiment.addReport(BatchCrossValidationReport.class);
		experiment.addReport(BatchRuntimeReport.class);

		// Run
		Lab.getInstance().run(experiment);
	}

	protected AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException {

		return createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class),
				createEngineDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE, LANGUAGE_CODE));
	}
}
