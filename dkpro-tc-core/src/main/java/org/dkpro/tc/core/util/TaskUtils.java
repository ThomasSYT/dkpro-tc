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
package org.dkpro.tc.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.FeatureExtractor;
import org.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import org.dkpro.tc.api.features.Instance;
import org.dkpro.tc.api.features.PairFeatureExtractor;
import org.dkpro.tc.api.type.JCasId;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.api.type.TextClassificationSequence;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.feature.InstanceIdFeature;
import org.dkpro.tc.core.ml.TcShallowLearningAdapter;
import org.dkpro.tc.core.task.uima.ExtractFeaturesConnector;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Utility methods needed in classification tasks (loading instances,
 * serialization of classifiers etc).
 */
public class TaskUtils {
	/**
	 * Loads the JSON file as a system resource, parses it and returnd the
	 * JSONObject.
	 *
	 * @param path
	 *            path to the config file
	 * @return the JSONObject containing all config parameters
	 * @throws IOException
	 *             in case of an error
	 */
	public static JSONObject getConfigFromJSON(String path) throws IOException {
		String jsonPath = FileUtils.readFileToString(new File(ClassLoader.getSystemResource(path).getFile()), "utf-8");
		return (JSONObject) JSONSerializer.toJSON(jsonPath);
	}

	/**
	 * Saves a serializable object of type T to disk. Output file may be
	 * uncompressed, gzipped or bz2-compressed. Compressed files must have a .gz
	 * or .bz2 suffix.
	 *
	 * @param serializedFile
	 *            model output file
	 * @param serializableObject
	 *            the object to serialize
	 * @throws IOException
	 *             in case of an error
	 */
	public static void serialize(File serializedFile, Object serializableObject) throws IOException {

		FileOutputStream fos = new FileOutputStream(serializedFile);
		BufferedOutputStream bufStr = new BufferedOutputStream(fos);

		OutputStream underlyingStream = null;
		if (serializedFile.getName().endsWith(".gz")) {
			underlyingStream = new GZIPOutputStream(bufStr);
		} else if (serializedFile.getName().endsWith(".bz2")) {
			underlyingStream = new CBZip2OutputStream(bufStr);
			// manually add bz2 prefix to make it compatible to normal bz2 tools
			// prefix has to be skipped when reading the stream with CBZip2
			fos.write("BZ".getBytes("UTF-8"));
		} else {
			underlyingStream = bufStr;
		}
		ObjectOutputStream serializer = new ObjectOutputStream(underlyingStream);
		try {
			serializer.writeObject(serializableObject);

		} finally {
			serializer.flush();
			serializer.close();
		}
	}

	/**
	 * Loads serialized Object from disk. File can be uncompressed, gzipped or
	 * bz2-compressed. Compressed files must have a .gz or .bz2 suffix.
	 *
	 * @param serializedFile
	 *            a file
	 * @param <T> 
	 * 			data type
	 * @return the deserialized Object
	 * @throws IOException
	 *             in case of an error
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> T deserialize(File serializedFile) throws IOException {
		FileInputStream fis = new FileInputStream(serializedFile);
		BufferedInputStream bufStr = new BufferedInputStream(fis);

		InputStream underlyingStream = null;
		if (serializedFile.getName().endsWith(".gz")) {
			underlyingStream = new GZIPInputStream(bufStr);
		} else if (serializedFile.getName().endsWith(".bz2")) {
			// skip bzip2 prefix that we added manually
			fis.read();
			fis.read();
			underlyingStream = new CBZip2InputStream(bufStr);
		} else {
			underlyingStream = bufStr;
		}

		ObjectInputStream deserializer = new ObjectInputStream(underlyingStream);

		Object deserializedObject = null;
		try {
			deserializedObject = deserializer.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException("The serialized file was probably corrupted.", e);
		} finally {
			deserializer.close();
		}
		return (T) deserializedObject;
	}

	public static Set<String> getRequiredTypesFromFeatureExtractors(List<ExternalResourceDescription> featureSet)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Set<String> requiredTypes = new HashSet<String>();

		for (ExternalResourceDescription element : featureSet) {

			String implName;
			if (element.getResourceSpecifier() instanceof CustomResourceSpecifier) {
				implName = ((CustomResourceSpecifier) element.getResourceSpecifier()).getResourceClassName();
			} else {
				implName = element.getImplementationName();
			}

			TypeCapability annotation = ReflectionUtil.getAnnotation(Class.forName(implName), TypeCapability.class);

			if (annotation != null) {
				requiredTypes.addAll(Arrays.asList(annotation.inputs()));
			}
		}

		return requiredTypes;
	}

	public static AnalysisEngineDescription getFeatureExtractorConnector(String outputPath, String dataWriter,
			String learningMode, String featureMode, boolean useSparseFeatures, boolean addInstanceId,
			boolean developerMode, boolean isTesting, boolean applyWeighting, List<String> filters,
			List<ExternalResourceDescription> extractorResources, String [] outcomes) throws ResourceInitializationException {
		List<Object> parameters = new ArrayList<>();
		parameters.addAll(Arrays.asList(ExtractFeaturesConnector.PARAM_ADD_INSTANCE_ID, addInstanceId,
				ExtractFeaturesConnector.PARAM_OUTPUT_DIRECTORY, outputPath,
				ExtractFeaturesConnector.PARAM_APPLY_WEIGHTING, applyWeighting,
				ExtractFeaturesConnector.PARAM_DATA_WRITER_CLASS, dataWriter,
				ExtractFeaturesConnector.PARAM_FEATURE_FILTERS, filters.toArray(new String[0]),
				ExtractFeaturesConnector.PARAM_FEATURE_MODE, featureMode,
				ExtractFeaturesConnector.PARAM_LEARNING_MODE, learningMode,
				ExtractFeaturesConnector.PARAM_IS_TESTING, isTesting,
				ExtractFeaturesConnector.PARAM_USE_SPARSE_FEATURES, useSparseFeatures,
				ExtractFeaturesConnector.PARAM_FEATURE_EXTRACTORS, extractorResources,
				ExtractFeaturesConnector.PARAM_OUTCOMES, outcomes));

		return AnalysisEngineFactory.createEngineDescription(ExtractFeaturesConnector.class,
				parameters.toArray());

	}

	/**
	 * Should not be called directly, but always from a connector (UIMA context
	 * with parameters initialized)
	 * 
	 * @param featureMode
	 *            the feature mode
	 * @param featureExtractors
	 *            feature extractors
	 * @param jcas
	 *            a jcas
	 * @param addInstanceId
	 *            instance id
	 * @param unit
	 *            the target
	 * @return an instance
	 * @throws TextClassificationException
	 *             in case of errors
	 */
	public static Instance getSingleInstanceUnit(String featureMode,
			FeatureExtractorResource_ImplBase[] featureExtractors, JCas jcas, 
			boolean addInstanceId, TextClassificationTarget unit) throws TextClassificationException {
		Instance instance = new Instance();
		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
		if (featureMode.equals(Constants.FM_UNIT)) {

			if (addInstanceId) {
				instance.addFeature(InstanceIdFeature.retrieve(jcas, unit));
			}

			for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
				if (!(featExt instanceof FeatureExtractor)) {
					throw new TextClassificationException(
							"Using non-unit FE in unit mode: " + featExt.getResourceName());
				}

				instance.setOutcomes(getOutcomes(jcas, unit));
				instance.setWeight(getWeight(jcas, unit));
				instance.setJcasId(jcasId);
				instance.addFeatures(((FeatureExtractor) featExt).extract(jcas, unit));
			}
		}
		return instance;
	}

	/**
	 * Should not be called directly, but always from a connector (UIMA context
	 * with parameters initialized)
	 * 
	 * @param featureMode
	 * 			the feature mode
	 * 
	 * @param featureExtractors
	 * 			the feature extractors for extracting the features
	 * 
	 * @param jcas
	 * 			the jcas object 
	 * 
	 * @param developerMode
	 * 			deactivates some sanity checks
	 * 
	 * @param addInstanceId
	 * 			if the instance id should be created as feature
	 * 
	 * @param supportSparseFeatures
	 * 			if sparse feature extraction is supported
	 * 
	 * @return instance
	 * 			an instance holding all extracted features for this cas object
	 * 
	 * @throws Exception
	 * 			in case of any error
	 */
	public static Instance getSingleInstance(String featureMode, FeatureExtractorResource_ImplBase[] featureExtractors,
			JCas jcas, boolean addInstanceId, boolean supportSparseFeatures) throws Exception {

		Instance instance = new Instance();

		if (featureMode.equals(Constants.FM_DOCUMENT)) {
			instance = getSingleInstanceDocument(instance, featureExtractors, jcas, addInstanceId,
					supportSparseFeatures);
		} else if (featureMode.equals(Constants.FM_PAIR)) {
			instance = getSingleInstancePair(instance, featureExtractors, jcas, addInstanceId);
		} else if (featureMode.equals(Constants.FM_UNIT)) {
			instance = getSingleInstanceUnit(instance, featureExtractors, jcas, addInstanceId, 
					supportSparseFeatures);
		}

		return instance;
	}

	/**
	 * @param instance
	 * 			an existing instance object
	 * 
	 * @param featureMode
	 * 			the feature mode
	 * 
	 * @param featureExtractors
	 * 			the feature extractors for extracting the features
	 * 
	 * @param jcas
	 * 			the jcas object 
	 * 
	 * @param developerMode
	 * 			deactivates some sanity checks
	 * 
	 * @param addInstanceId
	 * 			if the instance id should be created as feature
	 * 
	 * @param supportSparseFeatures
	 * 			if sparse feature extraction is supported
	 * 
	 * @return instance
	 * 			an instance holding all extracted features for this cas object
	 * 
	 * @throw exception
	 * 			in case of any error
	 */
	private static Instance getSingleInstanceUnit(Instance instance,
			FeatureExtractorResource_ImplBase[] featureExtractors, JCas jcas, boolean addInstanceId,
			boolean supportsSparseFeature) throws Exception {
		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
		TextClassificationTarget unit = JCasUtil.selectSingle(jcas, TextClassificationTarget.class);

		if (addInstanceId) {
			instance.addFeature(InstanceIdFeature.retrieve(jcas, unit));
		}

		for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
			if (!(featExt instanceof FeatureExtractor)) {
				throw new TextClassificationException("Using non-unit FE in unit mode: " + featExt.getResourceName());
			}

			if (supportsSparseFeature) {
				instance.addFeatures(getSparseFeatures(jcas, unit, featExt));
			} else {
				instance.addFeatures(getDenseFeatures(jcas, unit, featExt));
			}

			instance.setOutcomes(getOutcomes(jcas, unit));
			instance.setWeight(getWeight(jcas, unit));
			instance.setJcasId(jcasId);
		}
		return instance;
	}

	private static Instance getSingleInstancePair(Instance instance,
			FeatureExtractorResource_ImplBase[] featureExtractors, JCas jcas, boolean addInstanceId)
					throws TextClassificationException {
		try {
			int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
			if (addInstanceId) {
				instance.addFeature(InstanceIdFeature.retrieve(jcas));
			}

			for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
				if (!(featExt instanceof PairFeatureExtractor)) {
					throw new TextClassificationException(
							"Using non-pair FE in pair mode: " + featExt.getResourceName());
				}
				JCas view1 = jcas.getView(Constants.PART_ONE);
				JCas view2 = jcas.getView(Constants.PART_TWO);

				instance.setOutcomes(getOutcomes(jcas, null));
				instance.setWeight(getWeight(jcas, null));
				instance.setJcasId(jcasId);
				instance.addFeatures(((PairFeatureExtractor) featExt).extract(view1, view2));
			}
		} catch (CASException e) {
			throw new TextClassificationException(e);
		}
		return instance;
	}

	private static Instance getSingleInstanceDocument(Instance instance,
			FeatureExtractorResource_ImplBase[] featureExtractors, JCas jcas, boolean addInstanceId,
			boolean supportSparseFeatures) throws TextClassificationException {
		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();

		TextClassificationTarget documentTcu = JCasUtil.selectSingle(jcas, TextClassificationTarget.class);

		if (addInstanceId) {
			instance.addFeature(InstanceIdFeature.retrieve(jcas));
		}

		for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
			if (!(featExt instanceof FeatureExtractor)) {
				throw new TextClassificationException(
						"Using incompatible feature in document mode: " + featExt.getResourceName());
			}

			if (supportSparseFeatures) {
				instance.addFeatures(getSparseFeatures(jcas, documentTcu, featExt));
			} else {
				instance.addFeatures(getDenseFeatures(jcas, documentTcu, featExt));
			}

			instance.setOutcomes(getOutcomes(jcas, null));
			instance.setWeight(getWeight(jcas, null));
			instance.setJcasId(jcasId);
		}

		return instance;
	}

	public static List<Instance> getMultipleInstancesSequenceMode(FeatureExtractorResource_ImplBase[] featureExtractors,
			JCas jcas, boolean addInstanceId, boolean supportSparseFeatures) throws TextClassificationException {
		List<Instance> instances = new ArrayList<Instance>();

		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
		int sequenceId = 0;
		int unitId = 0;
		for (TextClassificationSequence seq : JCasUtil.select(jcas, TextClassificationSequence.class)) {
			unitId = 0;
			for (TextClassificationTarget unit : JCasUtil.selectCovered(jcas, TextClassificationTarget.class, seq)) {

				unit.setId(unitId++);

				Instance instance = new Instance();

				if (addInstanceId) {
					instance.addFeature(InstanceIdFeature.retrieve(jcas, unit, sequenceId));
				}

				// execute feature extractors and add features to instance

				for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
					if (!(featExt instanceof FeatureExtractor)) {
						throw new TextClassificationException(
								"Using non-unit FE in sequence mode: " + featExt.getResourceName());
					}
					if (supportSparseFeatures) {
						instance.addFeatures(getSparseFeatures(jcas, unit, featExt));
					} else {
						instance.addFeatures(getDenseFeatures(jcas, unit, featExt));
					}
				}

				// set and write outcome label(s)
				instance.setOutcomes(getOutcomes(jcas, unit));
				instance.setWeight(getWeight(jcas, unit));
				instance.setJcasId(jcasId);
				instance.setSequenceId(sequenceId);
				instance.setSequencePosition(unit.getId());

				instances.add(instance);
			}
			sequenceId++;
		}

		return instances;
	}

	private static Set<Feature> getDenseFeatures(JCas jcas, TextClassificationTarget unit,
			FeatureExtractorResource_ImplBase featExt) throws TextClassificationException {
		return ((FeatureExtractor) featExt).extract(jcas, unit);
	}

	private static Set<Feature> getSparseFeatures(JCas jcas, TextClassificationTarget unit,
			FeatureExtractorResource_ImplBase featExt) throws TextClassificationException {
		Set<Feature> features = ((FeatureExtractor) featExt).extract(jcas, unit);
		Set<Feature> filtered = new HashSet<>();
		for (Feature f : features) {
			if (!f.isDefaultValue()) {
				filtered.add(f);
			}
		}

		return filtered;
	}

	public static List<Instance> getMultipleInstancesUnitMode(FeatureExtractorResource_ImplBase[] featureExtractors,
			JCas jcas, boolean addInstanceId, boolean supportSparseFeatures) throws TextClassificationException {
		List<Instance> instances = new ArrayList<Instance>();
		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
		for (TextClassificationTarget unit : JCasUtil.select(jcas, TextClassificationTarget.class)) {

			Instance instance = new Instance();

			if (addInstanceId) {
				Feature feat = InstanceIdFeature.retrieve(jcas, unit);
				instance.addFeature(feat);
			}

			// execute feature extractors and add features to instance

			for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
				if (!(featExt instanceof FeatureExtractor)) {
					throw new TextClassificationException(
							"Using non-unit FE in sequence mode: " + featExt.getResourceName());
				}
				if (supportSparseFeatures) {
					instance.addFeatures(getSparseFeatures(jcas, unit, featExt));
				} else {
					instance.addFeatures(getDenseFeatures(jcas, unit, featExt));
				}
			}

			// set and write outcome label(s)
			instance.setOutcomes(getOutcomes(jcas, unit));
			instance.setWeight(getWeight(jcas, unit));
			instance.setJcasId(jcasId);
			// instance.setSequenceId(sequenceId);
			instance.setSequencePosition(unit.getId());

			instances.add(instance);
		}

		return instances;
	}

	public static List<Instance> getInstancesInSequence(FeatureExtractorResource_ImplBase[] featureExtractors,
			JCas jcas, TextClassificationSequence sequence, boolean addInstanceId, int sequenceId) throws Exception {
		List<Instance> instances = new ArrayList<Instance>();
		int jcasId = JCasUtil.selectSingle(jcas, JCasId.class).getId();
		for (TextClassificationTarget unit : JCasUtil.selectCovered(jcas, TextClassificationTarget.class, sequence)) {

			Instance instance = new Instance();

			if (addInstanceId) {
				instance.addFeature(InstanceIdFeature.retrieve(jcas, unit, sequenceId));
			}

			// execute feature extractors and add features to instance
			try {
				for (FeatureExtractorResource_ImplBase featExt : featureExtractors) {
					if (!(featExt instanceof FeatureExtractor)) {
						throw new TextClassificationException(
								"Feature extractors must implement the interface ["+ FeatureExtractor.class.getName() + "]: " + featExt.getResourceName());
					}
					instance.addFeatures(((FeatureExtractor) featExt).extract(jcas, unit));
				}
			} catch (TextClassificationException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// set and write outcome label(s)
			instance.setOutcomes(getOutcomes(jcas, unit));
			instance.setWeight(getWeight(jcas, unit));
			instance.setJcasId(jcasId);
			instance.setSequenceId(sequenceId);
			instance.setSequencePosition(unit.getId());

			instances.add(instance);
		}

		return instances;
	}

	public static List<String> getOutcomes(JCas jcas, AnnotationFS unit) throws TextClassificationException {
		Collection<TextClassificationOutcome> outcomes;
		if (unit == null) {
			outcomes = JCasUtil.select(jcas, TextClassificationOutcome.class);
		} else {
			outcomes = JCasUtil.selectCovered(jcas, TextClassificationOutcome.class, unit);
		}

		if (outcomes.size() == 0) {
			throw new TextClassificationException("No outcome annotations present in current CAS.");
		}

		List<String> stringOutcomes = new ArrayList<String>();
		for (TextClassificationOutcome outcome : outcomes) {
			stringOutcomes.add(outcome.getOutcome());
		}

		return stringOutcomes;
	}

	public static double getWeight(JCas jcas, AnnotationFS unit) throws TextClassificationException {
		Collection<TextClassificationOutcome> outcomes;
		if (unit == null) {
			outcomes = JCasUtil.select(jcas, TextClassificationOutcome.class);
		} else {
			outcomes = JCasUtil.selectCovered(jcas, TextClassificationOutcome.class, unit);
		}

		if (outcomes.size() == 0) {
			throw new TextClassificationException("No instance weight annotation present in current CAS.");
		}

		double weight = -1.0;
		for (TextClassificationOutcome outcome : outcomes) {
			weight = outcome.getWeight();
		}

		return weight;
	}
	
	public static TcShallowLearningAdapter getAdapter(List<Object> classificationArguments) throws ResourceInitializationException{
		
		if(classificationArguments == null || classificationArguments.size() < 0 ){
			throw new ResourceInitializationException(new IllegalArgumentException(
					"The classifcation arguments are empty or missing; The first element in the dimension ["
							+ Constants.DIM_CLASSIFICATION_ARGS
							+ "] has to be an instance of the machine learning adapter!"));
		}
		TcShallowLearningAdapter adapter = (TcShallowLearningAdapter) classificationArguments.get(0);
		
		return adapter;
	}
}