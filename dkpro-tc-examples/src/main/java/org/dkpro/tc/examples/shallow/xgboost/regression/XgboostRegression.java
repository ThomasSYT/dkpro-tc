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
package org.dkpro.tc.examples.shallow.xgboost.regression;

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
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.AvgSentenceRatioPerDocument;
import org.dkpro.tc.features.maxnormalization.AvgTokenRatioPerDocument;
import org.dkpro.tc.io.LinewiseTextOutcomeReader;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchRuntimeReport;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.xgboost.XgboostAdapter;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class XgboostRegression implements Constants {

	public static void main(String[] args) throws Exception {

		DemoUtils.setDkproHome(XgboostRegression.class.getSimpleName());

		ParameterSpace pSpace = getParameterSpace();

		XgboostRegression experiment = new XgboostRegression();
		experiment.runTrainTest(pSpace);
	}

	@SuppressWarnings("unchecked")
	public static ParameterSpace getParameterSpace() throws ResourceInitializationException {
		// configure training and test data reader dimension
		// train/test will use both, while cross-validation will only use the train part
		// The reader is also responsible for setting the labels/outcome on all
		// documents/instances it creates.
		Map<String, Object> dimReaders = new HashMap<String, Object>();

		CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
				LinewiseTextOutcomeReader.class, LinewiseTextOutcomeReader.PARAM_OUTCOME_INDEX, 0,
				LinewiseTextOutcomeReader.PARAM_TEXT_INDEX, 1, LinewiseTextOutcomeReader.PARAM_SOURCE_LOCATION,
				"src/main/resources/data/essays/train/essay_train.txt", LinewiseTextOutcomeReader.PARAM_LANGUAGE, "en");
		dimReaders.put(DIM_READER_TRAIN, readerTrain);

		CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
				LinewiseTextOutcomeReader.class, LinewiseTextOutcomeReader.PARAM_OUTCOME_INDEX, 0,
				LinewiseTextOutcomeReader.PARAM_TEXT_INDEX, 1, LinewiseTextOutcomeReader.PARAM_SOURCE_LOCATION,
				"src/main/resources/data/essays/test/essay_test.txt", LinewiseTextOutcomeReader.PARAM_LANGUAGE, "en");
		dimReaders.put(DIM_READER_TEST, readerTest);

		Dimension<List<Object>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
				Arrays.asList(new Object[] { new XgboostAdapter(), "booster=gbtree", "reg:linear" }));

		Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
				new TcFeatureSet(TcFeatureFactory.create(AvgSentenceRatioPerDocument.class),
						TcFeatureFactory.create(AvgTokenRatioPerDocument.class)));

		ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
				Dimension.create(DIM_LEARNING_MODE, LM_REGRESSION), Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT),
				dimFeatureSets, dimClassificationArgs);

		return pSpace;
	}

	// ##### TRAIN-TEST #####
	public void runTrainTest(ParameterSpace pSpace) throws Exception {
		ExperimentTrainTest experiment = new ExperimentTrainTest("XgboostRegression");
		experiment.setPreprocessing(getPreprocessing());
		experiment.setParameterSpace(pSpace);
		experiment.addReport(BatchTrainTestReport.class);
		experiment.addReport(ContextMemoryReport.class);
		experiment.addReport(BatchRuntimeReport.class);

		// Run
		Lab.getInstance().run(experiment);
	}

	protected AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException {
		return createEngineDescription(BreakIteratorSegmenter.class);
	}
}
