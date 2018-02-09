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
package org.dkpro.tc.io.libsvm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.Instance;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.io.DataWriter;

import com.google.gson.Gson;

/**
 * Format is outcome TAB index:value TAB index:value TAB ...
 * 
 * Zeros are omitted. Indexes need to be sorted.
 * 
 * For example: 1 1:1 3:1 4:1 6:1 2 2:1 3:1 5:1 7:1 1 3:1 5:1
 */
public class LibsvmDataFormatWriter implements DataWriter {

	public static final String INDEX2INSTANCEID = "index2Instanceid.txt";

	private File outputDirectory;

	boolean useSparse;

	private String learningMode;

	boolean applyWeighting;

	private File classifierFormatOutputFile;

	private BufferedWriter bw = null;

	private Map<String, String> index2instanceId;

	private Gson gson = new Gson();

	protected int maxId = 0;

	private Map<String, Integer> featureNames2id;

	private Map<String, Integer> outcomeMap;
	
	@Override
	public void writeGenericFormat(Collection<Instance> instances) throws Exception {
		initGeneric();

		// bulk-write - in sequence mode this keeps the instances together that
		// belong to the same sequence!
		Instance[] array = instances.toArray(new Instance[0]);
		bw.write(gson.toJson(array) + System.lineSeparator());

		bw.close();
		bw = null;
	}

	private void initGeneric() throws IOException {
		if (bw != null) {
			return;
		}
		bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(new File(outputDirectory, Constants.GENERIC_FEATURE_FILE), true), "utf-8"));
	}

	@Override
	public void transformFromGeneric() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(outputDirectory, Constants.GENERIC_FEATURE_FILE)), "utf-8"));

		String line = null;
		while ((line = reader.readLine()) != null) {
			Instance[] instance = gson.fromJson(line, Instance[].class);
			List<Instance> ins = new ArrayList<>(Arrays.asList(instance));
			writeClassifierFormat(ins);
		}

		reader.close();
		FileUtils.deleteQuietly(new File(outputDirectory, Constants.GENERIC_FEATURE_FILE));
	}

	@Override
	public void writeClassifierFormat(Collection<Instance> in) throws Exception {

		if (featureNames2id == null) {
			createFeatureNameMap();
		}

		initClassifierFormat();

		List<Instance> instances = new ArrayList<>(in);

		for (Instance instance : instances) {
			Map<Integer, Double> entry = new HashMap<>();
			recordInstanceId(instance, maxId++, index2instanceId);
			for (Feature f : instance.getFeatures()) {
				Integer id = featureNames2id.get(f.getName());
				Double val = toValue(f.getValue());

				if (Math.abs(val) < 0.00000001) {
					// skip zero values
					continue;
				}

				entry.put(id, val);
			}
			List<Integer> keys = new ArrayList<Integer>(entry.keySet());
			Collections.sort(keys);

			if (isRegression()) {
				bw.append(instance.getOutcome() + "\t");
			} else {
				bw.append(outcomeMap.get(instance.getOutcome()) + "\t");
			}
			
			bw.append(injectSequenceId(instance));
			
			for (int i = 0; i < keys.size(); i++) {
				Integer key = keys.get(i);
				Double value = entry.get(key);
				bw.append("" + key.toString() + ":" + value.toString());
				if (i + 1 < keys.size()) {
					bw.append("\t");
				}
			}
			bw.append("\n");
		}

		bw.close();
		bw = null;

		writeMapping(outputDirectory, INDEX2INSTANCEID, index2instanceId);
		writeFeatureName2idMapping(outputDirectory, AdapterFormat.getFeatureNameMappingFilename(), featureNames2id);
		writeOutcomeMapping(outputDirectory, AdapterFormat.getOutcomeMappingFilename(), outcomeMap);
	}

	protected String injectSequenceId(Instance instance) {
		
		// this method is a place holder for SvmHmm which uses an almost
		// identical format to the Libsvm format, except that it additionally
		// carries a sequence information. SvmHmm overloads this method.
		
		return "";
	}

	private void writeOutcomeMapping(File outputDirectory, String file, Map<String, Integer> map) throws IOException {
		
		if(isRegression()){
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for (String k : map.keySet()) {
			sb.append(k + "\t" + map.get(k) + "\n");
		}

		FileUtils.writeStringToFile(new File(outputDirectory, file), sb.toString(), "utf-8");
	}

	private Double toValue(Object value) {
		double v;
		if (value instanceof Number) {
			v = ((Number) value).doubleValue();
		} else {
			v = 1.0;
		}

		return v;
	}

	private void createFeatureNameMap() throws IOException {
		featureNames2id = new HashMap<>();
		List<String> readLines = FileUtils.readLines(new File(outputDirectory, Constants.FILENAME_FEATURES), "utf-8");

		// add a "bias" feature node; otherwise LIBLINEAR is unable to predict
		// the majority class for
		// instances consisting entirely of features never seen during training
		featureNames2id.put("x.BIAS", 1);

		Integer i = 2;
		for (String l : readLines) {
			if (l.isEmpty()) {
				continue;
			}
			featureNames2id.put(l, i++);
		}
	}

	private void writeFeatureName2idMapping(File outputDirectory2, String featurename2instanceid2,
			Map<String, Integer> stringToInt) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String k : stringToInt.keySet()) {
			sb.append(k + "\t" + stringToInt.get(k) + "\n");
		}
		FileUtils.writeStringToFile(new File(outputDirectory, featurename2instanceid2), sb.toString(), "utf-8");
	}

	private void initClassifierFormat() throws Exception {
		if (bw != null) {
			return;
		}

		bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(classifierFormatOutputFile, true), "utf-8"));
	}

	@Override
	public void init(File outputDirectory, boolean useSparse, String learningMode, boolean applyWeighting,
			String[] outcomes) throws Exception {
		this.outputDirectory = outputDirectory;
		this.useSparse = useSparse;
		this.learningMode = learningMode;
		this.applyWeighting = applyWeighting;
		classifierFormatOutputFile = new File(outputDirectory, Constants.FILENAME_DATA_IN_CLASSIFIER_FORMAT);

		index2instanceId = new HashMap<>();

		// Caution: DKPro Lab imports (aka copies!) the data of the train task
		// as test task. We use
		// appending mode for streaming. We might append the old training file
		// with
		// testing data!
		// Force delete the old training file to make sure we start with a
		// clean, empty file
		if (classifierFormatOutputFile.exists()) {
			FileUtils.forceDelete(classifierFormatOutputFile);
		}

		buildOutcomeMap(outcomes);
	}

	/**
	 * Creates a mapping from the label names to integer values to identify
	 * labels by integers
	 * 
	 * @param outcomes
	 */
	private void buildOutcomeMap(String[] outcomes) {
		if(isRegression()){
			return;
		}
		outcomeMap = new HashMap<>();
		Integer i = getStartIndexForOutcomeMap();
		List<String> outcomesSorted = new ArrayList<>(Arrays.asList(outcomes));
		Collections.sort(outcomesSorted);
		for (String o : outcomesSorted) {
			outcomeMap.put(o, i++);
		}
	}

	protected Integer getStartIndexForOutcomeMap() {
		//SvmHmm extension, which starts counting at 1
		return 0;
	}

	@Override
	public boolean canStream() {
		return true;
	}

	@Override
	public String getGenericFileName() {
		return Constants.GENERIC_FEATURE_FILE;
	}

	private void writeMapping(File outputDirectory, String fileName, Map<String, String> index2instanceId)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("#Index\tDkProInstanceId\n");
		for (String k : index2instanceId.keySet()) {
			sb.append(k + "\t" + index2instanceId.get(k) + "\n");
		}
		FileUtils.writeStringToFile(new File(outputDirectory, fileName), sb.toString(), "utf-8");
	}

	// build a map between the dkpro instance id and the index in the file
	private void recordInstanceId(Instance instance, int i, Map<String, String> index2instanceId) {
		Collection<Feature> features = instance.getFeatures();
		for (Feature f : features) {
			if (!f.getName().equals(Constants.ID_FEATURE_NAME)) {
				continue;
			}
			index2instanceId.put(i + "", f.getValue() + "");
			return;
		}
	}
	
	private boolean isRegression(){
		return learningMode.equals(Constants.LM_REGRESSION);
	}

	@Override
	public void close() throws Exception {

	}

}