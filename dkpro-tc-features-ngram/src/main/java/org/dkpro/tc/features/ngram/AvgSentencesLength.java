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
package org.dkpro.tc.features.ngram;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.FeatureExtractor;
import org.dkpro.tc.api.features.FeatureType;
import org.dkpro.tc.api.features.meta.MetaCollectorConfiguration;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.features.ngram.base.LuceneFeatureExtractorBase;
import org.dkpro.tc.features.ngram.meta.MaximumSentenceLengthMetaCollector;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Extracts the number of sentences in this classification unit
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence" })
public class AvgSentencesLength extends LuceneFeatureExtractorBase implements FeatureExtractor {

	public static final String FEATURE_NAME = "LuceneAvgSentenceLength";

	@Override
	public Set<Feature> extract(JCas jcas, TextClassificationTarget classificationUnit)
			throws TextClassificationException {

		long maxLen = getMaximumLength();

		double avgLength=0.0;
		
		List<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, classificationUnit);
		for (Sentence s : sentences) {
			List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, s);
			avgLength += getRatio(tokens.size(), maxLen);
		}

		avgLength /= sentences.size();

		return new Feature(FEATURE_NAME, avgLength , FeatureType.NUMERIC).asSet();
	}

	private double getRatio(int size, long maxLen) throws TextClassificationException {

		double value = (double) size / maxLen;

		if (value > 1.0) {
			// a larger value that during training was encountered; cap to 1.0
			value = 1.0;
		}

		if (value < 0) {
			throw new TextClassificationException("Negative sentence length encountered");
		}

		return value;
	}

	private long getMaximumLength() throws TextClassificationException {

		String string = "-1";
		try {
			string = getTopNgrams().getSampleWithMaxFreq().split("_")[0];
		} catch (ResourceInitializationException e) {
			throw new TextClassificationException(e);
		}
		return Long.valueOf(string);
	}

	@Override
	public List<MetaCollectorConfiguration> getMetaCollectorClasses(Map<String, Object> parameterSettings)
			throws ResourceInitializationException {

		return Arrays.asList(
				new MetaCollectorConfiguration(MaximumSentenceLengthMetaCollector.class, parameterSettings)
						.addStorageMapping(MaximumSentenceLengthMetaCollector.PARAM_TARGET_LOCATION,
								AvgSentencesLength.PARAM_SOURCE_LOCATION,
								MaximumSentenceLengthMetaCollector.LUCENE_DIR));
	}

	@Override
	protected String getFieldName() {
		return featureExtractorName;
	}

	@Override
	protected int getTopN() {
		return 1;
	}

	@Override
	protected String getFeaturePrefix() {
		return "sentLen";
	}
}
