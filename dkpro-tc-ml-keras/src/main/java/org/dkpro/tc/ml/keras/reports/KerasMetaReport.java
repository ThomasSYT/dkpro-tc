/*******************************************************************************
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
 ******************************************************************************/

package org.dkpro.tc.ml.keras.reports;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.tc.core.DeepLearningConstants;
import org.dkpro.tc.ml.report.TcAbstractReport;
import org.dkpro.tc.ml.report.util.SortedKeyProperties;

public class KerasMetaReport
    extends TcAbstractReport
    implements DeepLearningConstants
{

    @Override
    public void execute() throws Exception
    {
        String python = getDiscriminator(getContext(), DIM_PYTHON_INSTALLATION);

        String kerasVersion = getKerasVersion(python);
        String numpyVersion = getNumpyVersion(python);
        String tensorFlowVersion = getTensorflowVersion(python);
        String theanoVersion = getTheanoVersion(python);

        Properties p = new SortedKeyProperties();
        p.setProperty("KerasVersion", kerasVersion);
        p.setProperty("NumpyVersion", numpyVersion);
        p.setProperty("TensorFlowVersion", tensorFlowVersion);
        p.setProperty("TheanoVersion", theanoVersion);

        File file = getContext().getFile("softwareVersions.txt", AccessMode.READWRITE);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, "Version information");
        }
    }

    private String getTheanoVersion(String python) throws IOException, InterruptedException
    {
        return getVersion(python, "import theano; print(theano.__version__)");
    }

    private String getTensorflowVersion(String python) throws IOException, InterruptedException
    {
        return getVersion(python, "import tensorflow; print(tensorflow.__version__)");
    }

    private String getKerasVersion(String python) throws IOException, InterruptedException
    {
        return getVersion(python, "import keras; print(keras.__version__)");
    }

    private String getNumpyVersion(String python) throws IOException, InterruptedException
    {
        return getVersion(python, "import numpy; print(numpy.__version__)");
    }

    private String getVersion(String python, String cmd)
    {
        try {
            List<String> command = new ArrayList<>();
            command.add(python);
            command.add("-c");
            command.add(cmd);

            ProcessBuilder pb = new ProcessBuilder(command).command(command);
            Process start = pb.start();
            start.waitFor();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(start.getInputStream(), UTF_8))) {
                String l = null;
                while ((l = reader.readLine()) != null) {
                    output.add(l);
                }
            }

            return output.get(output.size() - 1);
        }
        catch (Exception e) {
            return "NotAvailable";
        }
    }

}