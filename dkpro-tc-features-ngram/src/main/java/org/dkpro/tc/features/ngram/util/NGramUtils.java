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
package org.dkpro.tc.features.ngram.util;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.toText;
import static org.dkpro.tc.core.Constants.NGRAM_GLUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.ColognePhonetic;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.tc.api.exception.TextClassificationException;

import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathException;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathFactory;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.CharacterNGramStringIterable;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringListIterable;

public class NGramUtils
{

    public static FrequencyDistribution<String> getAnnotationNgrams(JCas jcas,
            Annotation focusAnnotation, boolean lowerCaseNGrams, boolean filterPartialMatches,
            int minN, int maxN)
    {
        Set<String> empty = Collections.emptySet();
        return getAnnotationNgrams(jcas, focusAnnotation, lowerCaseNGrams, filterPartialMatches,
                minN, maxN, empty);
    }

    public static FrequencyDistribution<String> getAnnotationNgrams(JCas jcas,
            Annotation focusAnnotation, boolean lowerCaseNGrams, boolean filterPartialMatches,
            int minN, int maxN, Set<String> stopwords)
    {
        FrequencyDistribution<String> annoNgrams = new FrequencyDistribution<String>();

        // If the focusAnnotation contains sentence annotations, extract the ngrams sentence-wise
        // if not, extract them from all tokens in the focusAnnotation
        if (JCasUtil.selectCovered(jcas, Sentence.class, focusAnnotation).size() > 0) {
            for (Sentence s : selectCovered(jcas, Sentence.class, focusAnnotation)) {
                for (List<String> ngram : new NGramStringListIterable(
                        toText(selectCovered(Token.class, s)), minN, maxN)) {

                    if (lowerCaseNGrams) {
                        ngram = lower(ngram);
                    }

                    if (passesNgramFilter(ngram, stopwords, filterPartialMatches)) {
                        String ngramString = StringUtils.join(ngram, NGRAM_GLUE);
                        annoNgrams.inc(ngramString);
                    }
                }
            }
        }
        // FIXME the focus annotation branch doesn't make much sense
        else {
            for (List<String> ngram : new NGramStringListIterable(
                    toText(selectCovered(Token.class, focusAnnotation)), minN, maxN)) {

                if (lowerCaseNGrams) {
                    ngram = lower(ngram);
                }

                if (passesNgramFilter(ngram, stopwords, filterPartialMatches)) {
                    String ngramString = StringUtils.join(ngram, NGRAM_GLUE);
                    annoNgrams.inc(ngramString);
                }
            }
        }
        return annoNgrams;
    }

    /**
     * Convenience method to return document ngrams when there's no stopword list.
     * 
     * @param jcas
     *            a jcas
     * @param target
     *            the target span             
     * @param lowerCaseNGrams
     *            lower caseing
     * @param filterPartialMatches
     *            filter partial matches
     * @param minN
     *            minimal n
     * @param maxN
     *            maximal n
     * @return a frequency distribution
     * @throws TextClassificationException
     *         if an exception occurs
     */
    public static FrequencyDistribution<String> getDocumentNgrams(JCas jcas, Annotation target,
            boolean lowerCaseNGrams, boolean filterPartialMatches, int minN, int maxN)
                throws TextClassificationException
    {
        Set<String> empty = Collections.emptySet();
        return getDocumentNgrams(jcas, target, lowerCaseNGrams, filterPartialMatches, minN, maxN,
                empty);
    }

    /**
     * Convenience method to return document ngrams over Tokens.
     * 
     * @param jcas
     *            a jcas
     * @param target
     *            the target span            
     * @param lowerCaseNGrams
     *            lower caseing
     * @param filterPartialMatches
     *            filter partial matches
     * @param minN
     *            minimal n
     * @param maxN
     *            maximal n
     * @param stopwords
     *            set of stopwords
     * @return a frequency distribution
     */
    public static FrequencyDistribution<String> getDocumentNgrams(JCas jcas, Annotation target,
            boolean lowerCaseNGrams, boolean filterPartialMatches, int minN, int maxN,
            Set<String> stopwords)
                throws TextClassificationException
    {
        return getDocumentNgrams(jcas, target, lowerCaseNGrams, filterPartialMatches, minN, maxN,
                stopwords, Token.class);
    }

    /**
     * Returns document ngrams over any annotation type that extends Annotation. Intended use is
     * Lemma, Stem, etc.
     * 
     * @param jcas
     *            a jcas
     * @param target            
     *            target annotation span
     * @param lowerCaseNGrams
     *            lower caseing
     * @param filterPartialMatches
     *            filter partial matches
     * @param minN
     *            minimal n
     * @param maxN
     *            maximal n
     * @param stopwords
     *            set of stopwords
     * @param annotationClass
     *            annotation type of the ngram
     * @return a frequency distribution
     * 
     * @throws TextClassificationException
     *             when an exception occurs
     */
    public static FrequencyDistribution<String> getDocumentNgrams(JCas jcas, Annotation target,
            boolean lowerCaseNGrams, boolean filterPartialMatches, int minN, int maxN,
            Set<String> stopwords, Class<? extends Annotation> annotationClass)
                throws TextClassificationException
    {
        FrequencyDistribution<String> documentNgrams = new FrequencyDistribution<String>();
        for (Sentence s : selectCovered(jcas, Sentence.class, target)) {
            List<String> strings = valuesToText(jcas, s, annotationClass.getName());
            for (List<String> ngram : new NGramStringListIterable(strings, minN, maxN)) {
                if (lowerCaseNGrams) {
                    ngram = lower(ngram);
                }

                if (passesNgramFilter(ngram, stopwords, filterPartialMatches)) {
                    String ngramString = StringUtils.join(ngram, NGRAM_GLUE);
                    documentNgrams.inc(ngramString);
                }
            }
        }
        return documentNgrams;
    }

    public static FrequencyDistribution<String> getDocumentPosNgrams(JCas jcas, int minN, int maxN,
            boolean useCanonical)
    {
        FrequencyDistribution<String> posNgrams = new FrequencyDistribution<String>();
        for (Sentence s : select(jcas, Sentence.class)) {
            List<String> postagstrings = new ArrayList<String>();
            for (POS p : JCasUtil.selectCovered(jcas, POS.class, s)) {
                if (useCanonical) {
                    postagstrings.add(p.getClass().getSimpleName());
                }
                else {
                    postagstrings.add(p.getPosValue());
                }
            }
            String[] posarray = postagstrings.toArray(new String[postagstrings.size()]);

            for (List<String> ngram : new NGramStringListIterable(posarray, minN, maxN)) {
                posNgrams.inc(StringUtils.join(ngram, NGRAM_GLUE));

            }
        }
        return posNgrams;
    }

//    public static FrequencyDistribution<String> getDocumentPosNgrams(JCas jcas,
//            Annotation focusAnnotation, int minN, int maxN, boolean useCanonical)
//    {
//        FrequencyDistribution<String> posNgrams = new FrequencyDistribution<String>();
//
//        if (JCasUtil.selectCovered(jcas, Sentence.class, focusAnnotation).size() > 0) {
//            for (Sentence s : selectCovered(jcas, Sentence.class, focusAnnotation)) {
//                List<String> postagstrings = new ArrayList<String>();
//                for (POS p : JCasUtil.selectCovered(jcas, POS.class, s)) {
//                    if (useCanonical) {
//                        postagstrings.add(p.getClass().getSimpleName());
//                    }
//                    else {
//                        postagstrings.add(p.getPosValue());
//                    }
//                }
//                String[] posarray = postagstrings.toArray(new String[postagstrings.size()]);
//                for (List<String> ngram : new NGramStringListIterable(posarray, minN, maxN)) {
//                    posNgrams.inc(StringUtils.join(ngram, NGRAM_GLUE));
//                }
//            }
//        }
//        else {
//            List<String> postagstrings = new ArrayList<String>();
//            for (POS p : selectCovered(POS.class, focusAnnotation)) {
//                if (useCanonical) {
//                    postagstrings.add(p.getClass().getSimpleName());
//                }
//                else {
//                    postagstrings.add(p.getPosValue());
//                }
//            }
//            String[] posarray = postagstrings.toArray(new String[postagstrings.size()]);
//            for (List<String> ngram : new NGramStringListIterable(posarray, minN, maxN)) {
//                posNgrams.inc(StringUtils.join(ngram, NGRAM_GLUE));
//            }
//        }
//        return posNgrams;
//    }

    public static FrequencyDistribution<String> getDocumentPhoneticNgrams(JCas jcas,
            Annotation target, int minN, int maxN)
                throws TextClassificationException
    {
        StringEncoder encoder;
        String languageCode = jcas.getDocumentLanguage();

        if (languageCode.equals("en")) {
            encoder = new Soundex();
        }
        else if (languageCode.equals("de")) {
            encoder = new ColognePhonetic();
        }
        else {
            throw new TextClassificationException(
                    "Language code '" + languageCode + "' not supported by phonetic ngrams FE.");
        }

        FrequencyDistribution<String> phoneticNgrams = new FrequencyDistribution<String>();
        for (Sentence s : selectCovered(jcas, Sentence.class, target)) {
            List<String> phoneticStrings = new ArrayList<String>();
            for (Token t : JCasUtil.selectCovered(jcas, Token.class, s)) {
                try {
                    phoneticStrings.add(encoder.encode(t.getCoveredText()));
                }
                catch (EncoderException e) {
                    throw new TextClassificationException(e);
                }
            }
            String[] array = phoneticStrings.toArray(new String[phoneticStrings.size()]);

            for (List<String> ngram : new NGramStringListIterable(array, minN, maxN)) {
                phoneticNgrams.inc(StringUtils.join(ngram, NGRAM_GLUE));

            }
        }
        return phoneticNgrams;
    }

    public static FrequencyDistribution<String> getDocumentCharacterNgrams(JCas jcas,
            Annotation anno, boolean lowerCaseNgrams, int minN, int maxN)
    {
        FrequencyDistribution<String> charNgrams = new FrequencyDistribution<String>();
        String text = jcas.getDocumentText().substring(anno.getBegin(), anno.getEnd());
        for (String charNgram : new CharacterNGramStringIterable(text, minN, maxN)) {
            if (lowerCaseNgrams) {
                charNgram = charNgram.toLowerCase();
            }
            charNgrams.inc(charNgram);
        }

        return charNgrams;
    }

    /**
     * Creates a frequency distribution of character ngrams over the span of an annotation. The
     * boundary* parameter allows it to provide a string that is added additionally at the beginning
     * and end of the respective annotation span. If for instance the 'begin of sequence' or 'end of
     * sequence' of a span shall be marked the boundary parameter can be used. Provide an empty
     * character in case this parameters are not needed
     * 
     * @param focusAnnotation
     *            target span
     * @param lowerCaseNgrams
     *            use lower case
     * @param minN
     *            minimal n
     * @param maxN
     *            maximal n
     * @param boundaryBegin
     *            begin of boundary
     * @param boundaryEnd
     *            end of boundary
     * @return a frequency distribution
     */
    public static FrequencyDistribution<String> getAnnotationCharacterNgrams(
            Annotation focusAnnotation, boolean lowerCaseNgrams, int minN, int maxN,
            char boundaryBegin, char boundaryEnd)
    {
        FrequencyDistribution<String> charNgrams = new FrequencyDistribution<String>();
        for (String charNgram : new CharacterNGramStringIterable(
                boundaryBegin + focusAnnotation.getCoveredText() + boundaryEnd, minN, maxN)) {
            if (lowerCaseNgrams) {
                charNgram = charNgram.toLowerCase();
            }
            charNgrams.inc(charNgram);
        }

        return charNgrams;
    }

    /**
     * An ngram (represented by the list of tokens) does not pass the stopword filter: a)
     * filterPartialMatches=true - if it contains any stopwords b) filterPartialMatches=false - if
     * it entirely consists of stopwords
     * 
     * @param tokenList
     *            The list of tokens in a single ngram
     * @param stopwords
     *            The set of stopwords used for filtering
     * @param filterPartialMatches
     *            Whether ngrams where only parts are stopwords should also be filtered. For
     *            example, "United States of America" would be filtered, as it contains the stopword
     *            "of".
     * @return Whether the ngram (represented by the list of tokens) passes the stopword filter or
     *         not.
     */
    public static boolean passesNgramFilter(List<String> tokenList, Set<String> stopwords,
            boolean filterPartialMatches)
    {
        List<String> filteredList = new ArrayList<String>();
        for (String ngram : tokenList) {
            if (!stopwords.contains(ngram)) {
                filteredList.add(ngram);
            }
        }

        if (filterPartialMatches) {
            return filteredList.size() == tokenList.size();
        }
        else {
            return filteredList.size() != 0;
        }
    }

    public static FrequencyDistribution<String> getDocumentSkipNgrams(JCas jcas, Annotation anno,
            boolean lowerCaseNGrams, boolean filterPartialMatches, int minN, int maxN, int skipN,
            Set<String> stopwords)
    {
        FrequencyDistribution<String> documentNgrams = new FrequencyDistribution<String>();
        for (Sentence s : selectCovered(jcas, Sentence.class, anno)) {
            for (List<String> ngram : new SkipNgramStringListIterable(
                    toText(selectCovered(Token.class, s)), minN, maxN, skipN)) {
                if (lowerCaseNGrams) {
                    ngram = lower(ngram);
                }

                if (passesNgramFilter(ngram, stopwords, filterPartialMatches)) {
                    String ngramString = StringUtils.join(ngram, NGRAM_GLUE);
                    documentNgrams.inc(ngramString);
                }
            }
        }
        return documentNgrams;
    }

    public static FrequencyDistribution<String> getCharacterSkipNgrams(JCas jcas, Annotation target,
            boolean lowerCaseNGrams, int minN, int maxN, int skipN)
    {
        FrequencyDistribution<String> charNgrams = new FrequencyDistribution<String>();
        for (Token t : selectCovered(jcas, Token.class, target)) {
            String tokenText = t.getCoveredText();
            String[] charsTemp = tokenText.split("");
            String[] chars = new String[charsTemp.length + 1];
            for (int i = 0; i < charsTemp.length; i++) {
                chars[i] = charsTemp[i];
            }

            chars[0] = "^";
            chars[charsTemp.length] = "$";

            for (List<String> ngram : new SkipNgramStringListIterable(chars, minN, maxN, skipN)) {
                if (lowerCaseNGrams) {
                    ngram = lower(ngram);
                }

                String ngramString = StringUtils.join(ngram, NGRAM_GLUE);
                charNgrams.inc(ngramString);
            }
        }
        return charNgrams;
    }

    public static List<String> lower(List<String> ngram)
    {
        List<String> newNgram = new ArrayList<String>();
        for (String token : ngram) {
            newNgram.add(token.toLowerCase());
        }
        return newNgram;
    }

    public static <T extends Annotation> List<String> valuesToText(JCas jcas, Sentence s,
            String annotationClassName)
                throws TextClassificationException
    {
        List<String> texts = new ArrayList<String>();

        try {
            for (Entry<AnnotationFS, String> entry : FeaturePathFactory.select(jcas.getCas(),
                    annotationClassName)) {
                if (entry.getKey().getBegin() >= s.getBegin()
                        && entry.getKey().getEnd() <= s.getEnd()) {
                    texts.add(entry.getValue());
                }
            }
        }
        catch (FeaturePathException e) {
            throw new TextClassificationException(e);
        }
        return texts;
    }
}