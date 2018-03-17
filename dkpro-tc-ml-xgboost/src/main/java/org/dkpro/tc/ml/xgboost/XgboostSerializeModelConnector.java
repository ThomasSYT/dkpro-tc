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

package org.dkpro.tc.ml.xgboost;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.io.libsvm.LibsvmDataFormatSerializeModelConnector;

public class XgboostSerializeModelConnector
    extends LibsvmDataFormatSerializeModelConnector
    implements Constants
{

    @Override
    protected void trainModel(TaskContext aContext, File fileTrain) throws Exception
    {

        File model = new File(outputFolder, Constants.MODEL_CLASSIFIER);
        List<String> parameter = XgboostTestTask.getClassificationParameters(aContext,
                classificationArguments, learningMode);
        String content = XgboostTestTask.buildTrainConfigFile(fileTrain, model, parameter);

        File executable = XgboostTestTask.getExecutable();
        File configFile = XgboostTestTask.writeConfigFile(executable.getParentFile(), "train.conf",
                content);

        List<String> trainCommand = new ArrayList<>();
        trainCommand.add(executable.getAbsolutePath());
        trainCommand.add(configFile.getAbsolutePath());
        XgboostTestTask.runCommand(trainCommand);

        FileUtils.deleteQuietly(configFile);
    }

    @Override
    protected void writeAdapter() throws Exception
    {
        writeModelAdapterInformation(outputFolder, XgboostAdapter.class.getName());
    }

}