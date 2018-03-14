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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.pear.util.FileUtil;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.io.libsvm.LibsvmDataFormatTestTask;

import de.tudarmstadt.ukp.dkpro.core.api.resources.RuntimeProvider;

public class XgboostTestTask extends LibsvmDataFormatTestTask implements Constants {
	
	private static RuntimeProvider runtimeProvider = null;
	
	public static File getExecutable() throws Exception {

		if (runtimeProvider == null) {
			runtimeProvider = new RuntimeProvider("classpath:/org/dkpro/tc/ml/xgboost/");
		}

		return runtimeProvider.getFile("xgboost");
	}

	static List<String> getClassificationParameters(TaskContext aContext, List<Object> classificationArguments, String learningMode) throws IOException {
		List<String> parameters = new ArrayList<>();
		if (classificationArguments != null) {
			for (int i = 1; i < classificationArguments.size(); i++) {
				String a = (String) classificationArguments.get(i);
				parameters.add(a);
			}
		}
		
		if(!learningMode.equals(LM_REGRESSION)) {
			File folder = aContext.getFolder(OUTCOMES_INPUT_KEY, AccessMode.READONLY);
			File file = new File(folder, FILENAME_OUTCOMES);
			List<String> outcomes = FileUtils.readLines(file, "utf-8");
			parameters.add("num_class=" + outcomes.size() + "\n");
		}
		
		return parameters;
	}

	@Override
	protected Object trainModel(TaskContext aContext) throws Exception {

		File fileTrain = getTrainFile(aContext);
		File model = new File(aContext.getFolder("", AccessMode.READWRITE), Constants.MODEL_CLASSIFIER);
		
		List<String> parameters = getClassificationParameters(aContext, classificationArguments, learningMode);
		String configContent = buildTrainConfigFile(fileTrain, model, parameters);
		File executable = getExecutable();
		
		File configFile = writeConfigFile(executable.getParentFile(), "train.conf", configContent);
		
		List<String> trainCommand = new ArrayList<>();
		trainCommand.add(executable.getAbsolutePath());
		trainCommand.add(configFile.getAbsolutePath());
		runCommand(trainCommand);
		
		FileUtils.deleteQuietly(configFile);
		 
		return model;
	}
	
	static File writeConfigFile(File parentFile, String fileName, String content) throws Exception {
		File config = new File(parentFile, fileName);
		FileUtils.writeStringToFile(config, content, "utf-8");
		return config;
	}

	static void runCommand(List<String> aCommand) throws Exception {
		Process process = new ProcessBuilder().inheritIO().command(aCommand).start();
		process.waitFor();
	}
	
	public static String buildTrainConfigFile(File train, File model, List<String> parameter) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("task=train" + "\n");
		sb.append("data=" + train.getAbsolutePath() + "\n");
		sb.append("model_out=" + model.getAbsolutePath() + "\n");
		
		for(String p : parameter) {
			sb.append(p + "\n");
		}
		
		return sb.toString();
	}

	@Override
	protected void runPrediction(TaskContext aContext, Object model) throws Exception {
		
		File testFile = getTestFile(aContext);
		File prediction = createTemporaryPredictionFile();
		File executable = getExecutable();
		String content = buildTestConfigFile(testFile, (File) model, prediction);
		
		File file = writeConfigFile(executable.getParentFile(), "predict.conf", content);
		
		List<String> predictionCommand = new ArrayList<>();
		predictionCommand.add(executable.getAbsolutePath());
		predictionCommand.add(file.getAbsolutePath());
		runCommand(predictionCommand);
		
		mergePredictionWithGold(aContext, prediction);
	}
	
	static String buildTestConfigFile(File data, File model, File predictionOut) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("objective=multi:softmax" + "\n");
		sb.append("task=pred" + "\n");
		sb.append("test:data=" + data.getAbsolutePath() + "\n");
		sb.append("model_in=" + model.getAbsolutePath() + "\n");
		sb.append("name_pred=" + predictionOut.getAbsolutePath() + "\n");
		return sb.toString();
	}

	private void mergePredictionWithGold(TaskContext aContext, File tmpPrediction) throws Exception {
		
		File fileTest = getTestFile(aContext);
		File prediction = getPredictionFile(aContext);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prediction), "utf-8"));

			List<String> gold = readGoldValues(fileTest);
			List<String> pred = FileUtils.readLines(tmpPrediction, "utf-8");
			bw.write("#PREDICTION;GOLD" + "\n");
			for (int i = 0; i < gold.size(); i++) {
				String p = pred.get(i);
				String g = gold.get(i);
				bw.write(p + ";" + g);
				bw.write("\n");
			}
		} finally {
			IOUtils.closeQuietly(bw);
		}		
	}
	
	private List<String> readGoldValues(File f) throws Exception {
		List<String> goldValues = new ArrayList<>();
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
			
			String line=null;
			while((line=reader.readLine())!=null) {
				if(line.isEmpty()) {
					continue;
				}
				String[] split = line.split("\t");
				goldValues.add(split[0]);
			}
			
		}finally {
			IOUtils.closeQuietly(reader);
		}
		
		return goldValues;
	}

	static File createTemporaryPredictionFile() throws IOException {
		DateFormat df = new SimpleDateFormat("yyyyddMMHHmmss");
		Date today = Calendar.getInstance().getTime();
		String now = df.format(today);

		File createTempFile = FileUtil.createTempFile("xgboostPrediction" + now, ".txt");
		createTempFile.deleteOnExit();
		return createTempFile;
	}
}