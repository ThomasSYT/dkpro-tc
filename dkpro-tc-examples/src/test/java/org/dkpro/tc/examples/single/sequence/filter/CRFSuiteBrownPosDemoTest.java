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
package org.dkpro.tc.examples.single.sequence.filter;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.evaluation.Id2Outcome;
import org.dkpro.tc.evaluation.evaluator.EvaluatorBase;
import org.dkpro.tc.evaluation.evaluator.EvaluatorFactory;
import org.dkpro.tc.evaluation.measures.label.Accuracy;
import org.dkpro.tc.examples.single.sequence.CRFSuiteBrownPosDemoSimpleDkproReader;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.ml.crfsuite.CRFSuiteAdapter;
import org.dkpro.tc.ml.crfsuite.task.CRFSuiteTestTask;
import org.junit.Before;
import org.junit.Test;

/**
 * This test just ensures that the experiment runs without throwing any
 * exception.
 */
public class CRFSuiteBrownPosDemoTest  {
	CRFSuiteBrownPosDemoSimpleDkproReader javaExperiment;

	@Before
	public void setup() throws Exception {
		javaExperiment = new CRFSuiteBrownPosDemoSimpleDkproReader();
	}

	@Test
	public void testFeatureFilter() throws Exception {
		double runTrainTest = runTrainTestNoFilter();
		double runTrainTestFilter = runTrainTestFilter();

		// hard to tell what is suppose to happen - the data is too small to
		// learn anything robust - but, the numbers should differ
		assertTrue(Math.abs(runTrainTest - runTrainTestFilter) > 0.001);
	}

	@SuppressWarnings("unchecked")
	public Double runTrainTestNoFilter() throws Exception {
		// Random parameters for demonstration!
        //Number of iterations is set to an extreme low value (remove --> default: 100 iterations, or set accordingly)
		Dimension<List<String>> dimClassificationArgs = Dimension.create(Constants.DIM_CLASSIFICATION_ARGS,
				asList(new String[] { CRFSuiteAdapter.ALGORITHM_LBFGS, "-p", "max_iterations=5"}));
        ParameterSpace pSpace = CRFSuiteBrownPosDemoSimpleDkproReader.getParameterSpace(Constants.FM_SEQUENCE,
				Constants.LM_SINGLE_LABEL, dimClassificationArgs, null);

		ContextMemoryReport.key = CRFSuiteTestTask.class.getName();
		javaExperiment.runTrainTest(pSpace);

		Id2Outcome o = new Id2Outcome(ContextMemoryReport.id2outcome, Constants.LM_SINGLE_LABEL);
		EvaluatorBase createEvaluator = EvaluatorFactory.createEvaluator(o, true, false);
		Double results = createEvaluator.calculateEvaluationMeasures().get(Accuracy.class.getSimpleName());

		return results;
	}

	@SuppressWarnings("unchecked")
	public Double runTrainTestFilter() throws Exception {
		// Random parameters for demonstration!
		Dimension<List<String>> dimClassificationArgs = Dimension.create(Constants.DIM_CLASSIFICATION_ARGS,
				asList(CRFSuiteAdapter.ALGORITHM_ADAPTIVE_REGULARIZATION_OF_WEIGHT_VECTOR));

		Dimension<List<String>> dimFilter = Dimension.create(Constants.DIM_FEATURE_FILTERS,
				asList(FilterLuceneCharacterNgramStartingWithLetter.class.getName()));

		ParameterSpace pSpace = CRFSuiteBrownPosDemoSimpleDkproReader.getParameterSpace(Constants.FM_SEQUENCE,
				Constants.LM_SINGLE_LABEL, dimClassificationArgs, dimFilter);

		ContextMemoryReport.key = CRFSuiteTestTask.class.getName();
		javaExperiment.runTrainTest(pSpace);

		Id2Outcome o = new Id2Outcome(ContextMemoryReport.id2outcome, Constants.LM_SINGLE_LABEL);
		EvaluatorBase createEvaluator = EvaluatorFactory.createEvaluator(o, true, false);
		Double results = createEvaluator.calculateEvaluationMeasures().get(Accuracy.class.getSimpleName());

		return results;
	}
}
