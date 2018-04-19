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
package org.dkpro.tc.ml.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.ml.TcShallowLearningAdapter;

public class MLBackend
    implements Constants
{

    private TcShallowLearningAdapter adapter;
    private List<String> parametrization;

    public MLBackend(TcShallowLearningAdapter adapter, String... parametrization)
    {
        this.adapter = adapter;
        this.parametrization = new ArrayList<>(Arrays.asList(parametrization));
    }

    public TcShallowLearningAdapter getAdapter()
    {
        return adapter;
    }

    public List<String> getParametrization()
    {
        return parametrization;
    }

}
