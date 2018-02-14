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
package org.dkpro.tc.examples.deeplearning.keras.regression;

import static org.junit.Assert.assertTrue;

import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.examples.deeplearning.PythonLocator;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.ml.report.util.Tc2LtlabEvalConverter;
import org.junit.Test;

import de.unidue.ltl.evaluation.core.EvaluationData;
import de.unidue.ltl.evaluation.measures.correlation.SpearmanCorrelation;

public class KerasRegressionCrossValidation extends PythonLocator {
	@Test
	public void runTest() throws Exception {

		DemoUtils.setDkproHome(KerasRegressionWassa.class.getSimpleName());

		boolean testConditon = true;
		String python3 = null;
		try {
			python3 = getEnvironment();
		} catch (Exception e) {
			System.err.println("Failed to locate Python with Keras - will skip this test case");
			testConditon = false;
		}

		if (testConditon) {
			ParameterSpace ps = KerasRegression.getParameterSpace(python3);
			KerasRegression.runCrossValidation(ps);

			EvaluationData<Double> data = Tc2LtlabEvalConverter.convertRegressionModeId2Outcome(ContextMemoryReport.crossValidationCombinedIdFiles.get(0));
			SpearmanCorrelation spear = new SpearmanCorrelation(data);
			
			
			assertTrue(spear.getResult() > 0.1);
		}
	}
}