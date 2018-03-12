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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

	private List<String> getClassificationParameters() {
		List<String> parameters = new ArrayList<>();
		if (classificationArguments != null) {
			for (int i = 1; i < classificationArguments.size(); i++) {
				String a = (String) classificationArguments.get(i);
				parameters.add(a);
			}
		}
		return parameters;
	}

	private List<String> pickGold(List<String> readLines) {
		List<String> gold = new ArrayList<>();
		for (String l : readLines) {
			if (l.isEmpty()) {
				continue;
			}
			int indexOf = l.indexOf("\t");
			gold.add(l.substring(0, indexOf));
		}

		return gold;
	}

	private File createTemporaryPredictionFile() throws IOException {
		DateFormat df = new SimpleDateFormat("yyyyddMMHHmmss");
		Date today = Calendar.getInstance().getTime();
		String now = df.format(today);

		File createTempFile = FileUtil.createTempFile("libsvmPrediction" + now, ".libsvm");
		createTempFile.deleteOnExit();
		return createTempFile;
	}

	@Override
	protected Object trainModel(TaskContext aContext) throws Exception {

		File fileTrain = getTrainFile(aContext);
		File model = new File(aContext.getFolder("", AccessMode.READWRITE), Constants.MODEL_CLASSIFIER);
		
		List<String> parameters = getClassificationParameters();
		String configFile = buildConfigFile(fileTrain, model, parameters);
		
		File executable = getExecutable();
		File config = new File(executable.getParentFile(), "setup.conf");
		FileUtils.writeStringToFile(config, configFile, "utf-8");
		
		List<String> trainCommand = new ArrayList<>();
		trainCommand.add(executable.getAbsolutePath());
		trainCommand.add(config.getAbsolutePath());
		runTrain(trainCommand);
		
		FileUtils.deleteQuietly(config);
		 
		return model;
	}
	
	private void runTrain(List<String> aModelTrainCommand) throws Exception {
		Process process = new ProcessBuilder().inheritIO().command(aModelTrainCommand).start();
		process.waitFor();
	}
	
	public String buildConfigFile(File train, File model, List<String> parameter) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("task=train" + "\n");
		sb.append("data=" + train.getAbsolutePath() + "\n");
		sb.append("model_dir=" + model.getParentFile().getAbsolutePath() + "\n");
		sb.append("model_out=" + model.getName() + "\n");
		return sb.toString();
	}

	@Override
	protected void runPrediction(TaskContext aContext, Object model) throws Exception {
		File predFile = executeLibsvm(aContext, model);
		mergePredictionWithGold(aContext, predFile);
	}

	private void mergePredictionWithGold(TaskContext aContext, File predFile) throws Exception {
		
		File fileTest = getTestFile(aContext);
		File prediction = getPredictionFile(aContext);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prediction), "utf-8"));

			List<String> gold = pickGold(FileUtils.readLines(fileTest, "utf-8"));
			List<String> pred = FileUtils.readLines(predFile, "utf-8");
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

	private File executeLibsvm(TaskContext aContext, Object model) throws Exception {
//		File theModel = (File) model;
//		File fileTest = getTestFile(aContext);
//		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileTest), "utf-8"));
//		LibsvmPredict predictor = new LibsvmPredict();
//		File predTmp = createTemporaryPredictionFile();
//		
//		DataOutputStream output = null;
//		try {
//			output = new DataOutputStream(new FileOutputStream(predTmp));
//			svm_model svmModel = svm.svm_load_model(theModel.getAbsolutePath());
//			predictor.predict(r, output, svmModel, 0);
//		} finally {
//			IOUtils.closeQuietly(output);
//		}
//		
//		return predTmp;
		return null;
	}

}