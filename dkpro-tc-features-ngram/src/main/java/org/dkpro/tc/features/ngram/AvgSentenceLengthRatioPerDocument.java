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
import org.dkpro.tc.api.features.FeatureType;
import org.dkpro.tc.api.features.meta.MetaCollectorConfiguration;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.features.ngram.base.MaximumNormalizationExtractorBase;
import org.dkpro.tc.features.ngram.meta.MaxSentLenOverAllDocumentsMC;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Extracts the number of sentences in this classification unit
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence" })
public class AvgSentenceLengthRatioPerDocument extends MaximumNormalizationExtractorBase {

	public static final String FEATURE_NAME = "LuceneAvgSentenceLength";

	@Override
	public Set<Feature> extract(JCas jcas, TextClassificationTarget target)
			throws TextClassificationException {

		long maxLen = getMax();

		double avgLength=0.0;
		
		List<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, target);
		for (Sentence s : sentences) {
			List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, s);
			avgLength += getRatio(tokens.size(), maxLen);
		}

		avgLength /= sentences.size();

		return new Feature(FEATURE_NAME, avgLength , FeatureType.NUMERIC).asSet();
	}

	@Override
	public List<MetaCollectorConfiguration> getMetaCollectorClasses(Map<String, Object> parameterSettings)
			throws ResourceInitializationException {

		return Arrays.asList(
				new MetaCollectorConfiguration(MaxSentLenOverAllDocumentsMC.class, parameterSettings)
						.addStorageMapping(MaxSentLenOverAllDocumentsMC.PARAM_TARGET_LOCATION,
								AvgSentenceLengthRatioPerDocument.PARAM_SOURCE_LOCATION,
								MaxSentLenOverAllDocumentsMC.LUCENE_DIR));
	}

	@Override
	protected String getFieldName() {
		return MaxSentLenOverAllDocumentsMC.LUCENE_FIELD + featureExtractorName;
	}

	@Override
	protected int getTopN() {
		return 1;
	}

	@Override
	protected String getFeaturePrefix() {
		return getClass().getName();
	}
}
