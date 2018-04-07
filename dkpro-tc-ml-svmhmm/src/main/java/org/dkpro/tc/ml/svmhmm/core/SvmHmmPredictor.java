/**
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
 */
package org.dkpro.tc.ml.svmhmm.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.resources.RuntimeProvider;

public class SvmHmmPredictor
    extends SvmHmm
{
    private static RuntimeProvider runtimeProvider;

    public void predict(File data, File model, File predictionOutput) throws Exception
    {
        List<String> command = buildPredictionCommand(data, model, predictionOutput);
        runCommand(command);
    }

    public static File getPredictionExecutable() throws Exception
    {

        if (runtimeProvider == null) {
            runtimeProvider = new RuntimeProvider("classpath:/org/dkpro/tc/ml/svmhmm/");
        }

        return runtimeProvider.getFile("svm_hmm_classify");
    }

    public static List<String> buildPredictionCommand(File testFile, File modelLocation,
            File outputPredictions)
        throws Exception
    {
        List<String> result = new ArrayList<>();

        result.add(getPredictionExecutable().getAbsolutePath());
        result.add(testFile.getAbsolutePath());
        result.add(modelLocation.getAbsolutePath());
        result.add(outputPredictions.getAbsolutePath());

        return result;
    }

}
