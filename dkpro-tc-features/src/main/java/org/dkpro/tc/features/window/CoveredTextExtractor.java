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
package org.dkpro.tc.features.window;

import org.apache.uima.fit.descriptor.TypeCapability;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@TypeCapability(inputs = { 
"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token"})
public class CoveredTextExtractor extends WindowFeatureExtractor<Token>{

	@Override
	protected Class<Token> getTargetType() {
		return Token.class;
	}

	@Override
	protected String getFeatureName() {		
		return "Token";
	}

	@Override
	protected String getFeatureValue(Token a) {
				
		return a.getCoveredText().trim();
	}


}
