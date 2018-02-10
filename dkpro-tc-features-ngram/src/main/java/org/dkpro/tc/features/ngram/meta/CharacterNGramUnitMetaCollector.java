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
package org.dkpro.tc.features.ngram.meta;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.features.ngram.util.NGramUtils;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;

/**
 * This meta collector should be used if (i) the JCas contains either one target annotation which
 * covers only a subset of the document text or several targets exist and (ii) you wish that the
 * information in the resulting frequency distribution contains only text that is covered by those
 * frequency distributions. If you have only one target which spans over the entire document text 0
 * to document-length than you should use {@link org.dkpro.tc.features.ngram.meta.CharacterNGramMetaCollector}
 */
public class CharacterNGramUnitMetaCollector
    extends LuceneMetaCollector
{

    @ConfigurationParameter(name = CharacterNGram.PARAM_NGRAM_MIN_N, mandatory = true, defaultValue = "1")
    private int ngramMinN;

    @ConfigurationParameter(name = CharacterNGram.PARAM_NGRAM_MAX_N, mandatory = true, defaultValue = "3")
    private int ngramMaxN;

    @ConfigurationParameter(name = CharacterNGram.PARAM_NGRAM_LOWER_CASE, mandatory = false, defaultValue = "true")
    private String stringLowerCase;
    
    boolean lowerCase = true;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        lowerCase = Boolean.valueOf(stringLowerCase);
        
    }

    @Override
    protected FrequencyDistribution<String> getNgramsFD(JCas jcas)
    {
        FrequencyDistribution<String> fd = new FrequencyDistribution<String>();
        for (Annotation a : JCasUtil.select(jcas, TextClassificationTarget.class)) {
            FrequencyDistribution<String> ngramDist = NGramUtils.getAnnotationCharacterNgrams(a,
                    lowerCase, ngramMinN, ngramMaxN, '^', '$');
            for (String condition : ngramDist.getKeys()) {
                fd.addSample(condition, ngramDist.getCount(condition));
            }
        }
        return fd;
    }

    @Override
    protected String getFieldName()
    {
        return CharacterNGram.LUCENE_NGRAM_FIELD + featureExtractorName;
    }
}