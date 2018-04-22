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
package org.dkpro.tc.examples.shallow.misc.filter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.dkpro.tc.api.features.Instance;
import org.dkpro.tc.core.feature.filter.FeatureFilter;

import com.google.gson.Gson;

/**
 * Resamples the instances in order to achieve a uniform class distribution. If the class
 * distribution is already uniform, nothing is changed. In all other cases, this results in dropping
 * some instances.
 * 
 * FIXME: This is currently optimized for memory consumption and might be slow for large feature
 * stores. If there is enough memory (at least 2x the size of the current feature store) a time
 * optimized version should simply create a new feature store that only holds the selected
 * instances. In the worst case, this would double memory consumption.
 */
public class UniformClassDistributionFilter
    implements FeatureFilter
{

    @Override
    public void applyFilter(File f) throws Exception
    {
        Map<String, List<Integer>> outcomeLineMap = new HashMap<>();
        Gson gson = new Gson();

        int lineId = 0;
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
            while ((line = reader.readLine()) != null) {
                Instance[] ins = gson.fromJson(line, Instance[].class);
                for (Instance i : ins) {
                    List<Integer> list = outcomeLineMap.get(i.getOutcome());
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(lineId++);
                    outcomeLineMap.put(i.getOutcome(), list);
                }
            }
        }
        finally {
            IOUtils.closeQuietly(reader);
        }

        // find the smallest class
        int minClassSize = Integer.MAX_VALUE;
        String minOutcome = null;
        for (Entry<String, List<Integer>> e : outcomeLineMap.entrySet()) {
            int size = e.getValue().size();
            if (size < minClassSize) {
                minClassSize = size;
                minOutcome = e.getKey();
            }
        }

        // shuffle the line-ids und shrink lists to minimal size
        for (Entry<String, List<Integer>> e : outcomeLineMap.entrySet()) {
            List<Integer> list = e.getValue();
            Collections.shuffle(list);
            outcomeLineMap.put(e.getKey(), list.subList(0, minClassSize));
        }

        File tmpOut = new File(f.getParentFile(), "json_filtered.txt");
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tmpOut), "utf-8"));

            line = null;
            lineId = 0;
            while ((line = reader.readLine()) != null) {
                Instance[] ins = gson.fromJson(line, Instance[].class);
                for (Instance i : ins) {
                    // write the minimal class
                    if (minOutcome.equals(i.getOutcome())) {
                        writer.write(line + "\n");
                        lineId++;
                        continue;
                    }

                    boolean write = outcomeLineMap.get(i.getOutcome()).contains(lineId);
                    if (write) {
                        writer.write(line + "\n");
                    }
                }
                lineId++;
            }
        }
        finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }

        FileUtils.copyFile(tmpOut, f);
        FileUtils.deleteQuietly(tmpOut);
    }

    @Override
    public boolean isApplicableForTraining()
    {
        return true;
    }

    @Override
    public boolean isApplicableForTesting()
    {
        return false;
    }
}