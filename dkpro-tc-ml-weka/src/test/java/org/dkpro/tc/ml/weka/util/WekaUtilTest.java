/**
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.dkpro.tc.ml.weka.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.FeatureType;
import org.dkpro.tc.api.features.Instance;
import org.junit.Test;

import weka.core.Attribute;
import weka.core.Instances;

public class WekaUtilTest
{

    @Test
    public void instanceToArffTest()
        throws Exception
    {

        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature2", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));
        i1.addFeature(new Feature("feature4", Values.VALUE_1, FeatureType.NUMERIC));
        i1.setOutcomes("1");

        Instance i2 = new Instance();
        i2.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature2", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));
        i2.addFeature(new Feature("feature4", Values.VALUE_2, FeatureType.NUMERIC));
        i2.setOutcomes("2");

        Instance i3 = new Instance();
        i3.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i3.addFeature(new Feature("feature2", 1, FeatureType.NUMERIC));
        i3.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));
        i3.addFeature(new Feature("feature4", Values.VALUE_3, FeatureType.NUMERIC));
        i3.setOutcomes("2");

        List<Instance> iList = new ArrayList<>();
        iList.add(i1);
        iList.add(i2);
        iList.add(i3);

        File outfile = new File("target/test/out.txt");
        outfile.mkdirs();
        outfile.createNewFile();
        outfile.deleteOnExit();

        WekaUtils.instanceListToArffFile(outfile, iList);

        System.out.println(FileUtils.readFileToString(outfile, "utf-8"));
    }

    @Test
    public void instanceToArffTest_multiLabel()
        throws Exception
    {
        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature2", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));
        i1.addFeature(new Feature("feature4", Values.VALUE_1, FeatureType.NUMERIC));
        i1.setOutcomes("1", "2");

        Instance i2 = new Instance();
        i2.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature2", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));
        i2.addFeature(new Feature("feature4", Values.VALUE_2, FeatureType.NUMERIC));
        i2.setOutcomes("2", "3");

        Instance i3 = new Instance();
        i3.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i3.addFeature(new Feature("feature2", 1, FeatureType.NUMERIC));
        i3.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));
        i3.addFeature(new Feature("feature4", Values.VALUE_3, FeatureType.NUMERIC));
        i3.setOutcomes("2");

        List<Instance> instances = new ArrayList<>();
        instances.add(i1);
        instances.add(i2);
        instances.add(i3);

        File outfile = new File("target/test/out.txt");
        outfile.mkdirs();
        outfile.createNewFile();
        outfile.deleteOnExit();

        WekaUtils.instanceListToArffFileMultiLabel(outfile, instances, false);

        System.out.println(FileUtils.readFileToString(outfile, "utf-8"));
    }

    @Test
    public void tcInstanceToWekaInstanceRegressionTest()
        throws Exception
    {

        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature2", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));

        Instance i2 = new Instance();
        i2.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature4", "val_1", FeatureType.STRING));
        i2.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));

        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("feature5"));
        attributes.add(new Attribute("feature2"));
        attributes.add(new Attribute("feature4", Arrays.asList(new String[] { "val_1", "val_2" })));
        attributes.add(new Attribute("feature1"));
        attributes.add(new Attribute("outcome"));
        
        Instances trainingData = new Instances("test", attributes, 0);

        weka.core.Instance wekaInstance1 = WekaUtils.tcInstanceToWekaInstance(i1, trainingData,
                null, true);
        weka.core.Instance wekaInstance2 = WekaUtils.tcInstanceToWekaInstance(i2, trainingData,
                null, true);

        assertEquals(true, wekaInstance1.equalHeaders(wekaInstance2));
        assertEquals(5, wekaInstance1.numAttributes());

        wekaInstance1.dataset().add(wekaInstance1);
        wekaInstance2.dataset().add(wekaInstance2);
        System.out.println(wekaInstance1.dataset() + "\n");
        System.out.println(wekaInstance2.dataset() + "\n");
    }

    @Test
    public void tcInstanceToWekaInstanceTest()
        throws Exception
    {
        List<String> outcomeValues = Arrays.asList(new String[] { "outc_1", "outc_2", "outc_3" });

        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature2", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));

        Instance i2 = new Instance();
        i2.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature4", "val_1", FeatureType.STRING));
        i2.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));

        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("feature5"));
        attributes.add(new Attribute("feature2"));
        attributes.add(new Attribute("feature4", Arrays.asList(new String[] { "val_1", "val_2" })));
        attributes.add(new Attribute("feature1"));
        attributes.add(new Attribute("outcome", outcomeValues));
        
        Instances trainingData = new Instances("test", attributes, 0);

        weka.core.Instance wekaInstance1 = WekaUtils.tcInstanceToWekaInstance(i1, trainingData,
                outcomeValues, false);
        weka.core.Instance wekaInstance2 = WekaUtils.tcInstanceToWekaInstance(i2, trainingData,
                outcomeValues, false);

        assertEquals(true, wekaInstance1.equalHeaders(wekaInstance2));
        assertEquals(5, wekaInstance1.numAttributes());

        wekaInstance1.dataset().add(wekaInstance1);
        wekaInstance2.dataset().add(wekaInstance2);
        System.out.println(wekaInstance1.dataset() + "\n");
        System.out.println(wekaInstance2.dataset() + "\n");
    }

    @Test
    public void tcInstanceToMekaInstanceTest()
        throws Exception
    {
        List<String> outcomeValues = Arrays.asList(new String[] { "outc_1", "outc_2", "outc_3" });

        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature2", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));

        Instance i2 = new Instance();
        i2.addFeature(new Feature("feature1", 1, FeatureType.NUMERIC));
        i2.addFeature(new Feature("feature4", "val_1", FeatureType.STRING));
        i2.addFeature(new Feature("feature3_{{", "b", FeatureType.STRING));

        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("outc_1", Arrays.asList(new String[] { "0", "1" })));
        attributes.add(new Attribute("outc_2", Arrays.asList(new String[] { "0", "1" })));
        attributes.add(new Attribute("outc_3", Arrays.asList(new String[] { "0", "1" })));
        attributes.add(new Attribute("feature5"));
        attributes.add(new Attribute("feature2"));
        attributes.add(new Attribute("feature4", Arrays.asList(new String[] { "val_1", "val_2" })));
        attributes.add(new Attribute("feature1"));

        Instances trainingData = new Instances("test", attributes, 0);

        weka.core.Instance wekaInstance1 = WekaUtils.tcInstanceToMekaInstance(i1, trainingData,
                outcomeValues);
        weka.core.Instance wekaInstance2 = WekaUtils.tcInstanceToMekaInstance(i2, trainingData,
                outcomeValues);

        assertEquals(true, wekaInstance1.equalHeaders(wekaInstance2));
        assertEquals(7, wekaInstance1.numAttributes());

        wekaInstance1.dataset().add(wekaInstance1);
        wekaInstance2.dataset().add(wekaInstance2);
        System.out.println(wekaInstance1.dataset() + "\n");
        System.out.println(wekaInstance2.dataset() + "\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tcInstanceToWekaInstanceFailTest()
        throws Exception
    {
        List<String> outcomeValues = Arrays.asList(new String[] { "outc_1", "outc_2", "outc_3" });

        Instance i1 = new Instance();
        i1.addFeature(new Feature("feature1", 2, FeatureType.NUMERIC));
        i1.addFeature(new Feature("feature4", "val_1", FeatureType.STRING));
        i1.addFeature(new Feature("feature3_{{", "a", FeatureType.STRING));

        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("feature2"));
        attributes.add(new Attribute("feature4", Arrays.asList(new String[] { "val_4", "val_2" })));
        attributes.add(new Attribute("outcome", outcomeValues));
        
        Instances trainingData = new Instances("test", attributes, 0);

        @SuppressWarnings("unused")
        weka.core.Instance wekaInstance1 = WekaUtils.tcInstanceToWekaInstance(i1, trainingData,
                outcomeValues, false);
    }

    private enum Values
    {
        VALUE_1, VALUE_2, VALUE_3
    }
    
    @Test
    public void makeOutcomeClassesCompatibleTest()
        throws Exception
    {
    	Instances train = WekaUtils.getInstances(new File("src/test/resources/utils/train.arff"), false);
    	Instances test = WekaUtils.getInstances(new File("src/test/resources/utils/test.arff"), false);
    	
    	Instances testCompatible = WekaUtils.makeOutcomeClassesCompatible(train, test, false);
    	
    	System.out.println(WekaUtils.getClassLabels(testCompatible, false));
    	assertEquals(2, WekaUtils.getClassLabels(testCompatible, false).size());
    }
}
