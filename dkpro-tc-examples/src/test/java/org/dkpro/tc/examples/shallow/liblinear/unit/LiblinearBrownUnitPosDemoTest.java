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
package org.dkpro.tc.examples.shallow.liblinear.unit;

import static org.junit.Assert.assertEquals;

import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.examples.TestCaseSuperClass;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.ml.report.util.Tc2LtlabEvalConverter;
import org.junit.Before;
import org.junit.Test;

import de.unidue.ltl.evaluation.core.EvaluationData;
import de.unidue.ltl.evaluation.measures.Accuracy;

/**
 * This test just ensures that the experiment runs without throwing
 * any exception.
 */
public class LiblinearBrownUnitPosDemoTest extends TestCaseSuperClass
{
    LiblinearBrownUnitSimpleDkproReader javaExperiment;
    ParameterSpace pSpace;

    @Before
    public void setup()
        throws Exception
    {
        super.setup();
        
        javaExperiment = new LiblinearBrownUnitSimpleDkproReader();
        pSpace = LiblinearBrownUnitSimpleDkproReader.getParameterSpace();
    }

    @Test
    public void testTrainTest()
        throws Exception
    {
        javaExperiment.runTrainTest(pSpace);
        
        EvaluationData<String> data = Tc2LtlabEvalConverter.convertSingleLabelModeId2Outcome(ContextMemoryReport.id2outcomeFiles.get(0));
        Accuracy<String> acc = new Accuracy<String>(data);
        
        assertEquals(0.76, acc.getResult(), 0.01);
    }
}
