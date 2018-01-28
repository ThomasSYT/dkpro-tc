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
package org.dkpro.tc.examples.single.document;

import static org.junit.Assert.assertEquals;

import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.examples.TestCaseSuperClass;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.weka.task.WekaTestTask;
import org.junit.Before;
import org.junit.Test;

import de.unidue.ltl.evaluation.measures.Accuracy;
import de.unidue.ltl.evaluation.util.convert.DKProTcDataFormatConverter;

/**
 * This test just ensures that the experiment runs without throwing
 * any exception.
 */
public class WekaTwentyNewsgroupsDemoTest extends TestCaseSuperClass
{
    WekaTwentyNewsgroupsDemo javaExperiment;
    ParameterSpace pSpace;

    @Before
    public void setup()
        throws Exception
    {
        super.setup();
        
        javaExperiment = new WekaTwentyNewsgroupsDemo();
        pSpace = WekaTwentyNewsgroupsDemo.getParameterSpace();
    }

    @Test
    public void testJavaCrossValidation()
        throws Exception
    {
        ContextMemoryReport.key = ExperimentCrossValidation.class.getName();
        javaExperiment.runCrossValidation(pSpace);
        
    	Accuracy<String> accuracy = new Accuracy<>(
				DKProTcDataFormatConverter.convertSingleLabelModeId2Outcome(ContextMemoryReport.id2outcome));
        
        assertEquals(0.625, accuracy.getResult(), 0.0001);
    }

    @Test
    public void testJavaTrainTest()
        throws Exception
    {
        ContextMemoryReport.key = WekaTestTask.class.getName();
        javaExperiment.runTrainTest(pSpace);
        
    	Accuracy<String> accuracy = new Accuracy<>(
				DKProTcDataFormatConverter.convertSingleLabelModeId2Outcome(ContextMemoryReport.id2outcome));
        assertEquals(0.625, accuracy.getResult(), 0.0001);
    }
}
