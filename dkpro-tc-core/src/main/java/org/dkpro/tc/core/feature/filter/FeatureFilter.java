/*******************************************************************************
 * Copyright 2016
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
package org.dkpro.tc.core.feature.filter;

import java.io.File;

/**
 * Filter for feature store
 */
public interface FeatureFilter
{

    /**
     * Applies the filter to the given feature store
     * 
     * @param tmpFeatureFile
     *            the file to filter
     * @throws Exception
     *             in case of error
     */
    void applyFilter(File tmpFeatureFile) throws Exception;

    /**
     * Whether the filter is applicable on training instances
     * 
     * @return boolean value
     */
    boolean isApplicableForTraining();

    /**
     * Whether the filter is applicable on testing instances
     * 
     * @return boolean value
     */
    boolean isApplicableForTesting();

}
