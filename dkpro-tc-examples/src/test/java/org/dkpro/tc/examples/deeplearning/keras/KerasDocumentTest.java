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
package org.dkpro.tc.examples.deeplearning.keras;

import static org.junit.Assert.assertTrue;

import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.evaluation.Id2Outcome;
import org.dkpro.tc.evaluation.evaluator.EvaluatorBase;
import org.dkpro.tc.evaluation.evaluator.EvaluatorFactory;
import org.dkpro.tc.evaluation.measures.label.Accuracy;
import org.dkpro.tc.examples.deeplearning.KerasLocator;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.ml.keras.KerasTestTask;
import org.junit.Test;

public class KerasDocumentTest extends KerasLocator {
	@Test
	public void runTest() throws Exception {

		DemoUtils.setDkproHome(KerasDocumentTrainTest.class.getSimpleName());

		ContextMemoryReport.key = KerasTestTask.class.getName();

		boolean testConditon = true;
		String python3 = null;
		try {
			python3 = getEnvironment();
		} catch (Exception e) {
			System.err.println("Failed to locate Python with Keras - will skip this test case");
			testConditon = false;
		}
		
		if (testConditon) {
			ParameterSpace ps = KerasDocumentTrainTest.getParameterSpace(python3);
			KerasDocumentRegression.runTrainTest(ps);

			Id2Outcome o = new Id2Outcome(ContextMemoryReport.id2outcome, Constants.LM_SINGLE_LABEL);
			EvaluatorBase createEvaluator = EvaluatorFactory.createEvaluator(o, true, false);
			Double result = createEvaluator.calculateEvaluationMeasures().get(Accuracy.class.getSimpleName());
			assertTrue(result > 0.2);
		}
	}
}
