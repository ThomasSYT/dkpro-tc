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
package org.dkpro.tc.ml.weka.report;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.tc.ml.report.TcBatchReportBase;
import org.dkpro.tc.ml.report.util.SortedKeyProperties;
import org.dkpro.tc.ml.weka.task.WekaTestTask;
import org.dkpro.tc.ml.weka.util.MultilabelResult;
import org.dkpro.tc.ml.weka.util.WekaUtils;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Writes a instanceId / outcome data for each classification instance.
 */
public class WekaOutcomeIDReport
    extends TcBatchReportBase
{
    /**
     * Character that is used for separating fields in the output file
     */
    public static final String SEPARATOR_CHAR = ";";

    private File mlResults;
    
    public WekaOutcomeIDReport()
    {
        // required by groovy
    }

	
	boolean isRegression;
	boolean isUnit;
	boolean isMultiLabel;

	protected void init() {
		isRegression = getDiscriminator(getContext(), DIM_LEARNING_MODE).equals(LM_REGRESSION);
		isUnit = getDiscriminator(getContext(), DIM_FEATURE_MODE).equals(FM_UNIT);
		isMultiLabel = getDiscriminator(getContext(), DIM_LEARNING_MODE).equals(LM_MULTI_LABEL);
	}

	@Override
	public void execute() throws Exception {
		
		init();
		
        File arff = WekaUtils.getFile(getContext(), "",
                FILENAME_PREDICTIONS, AccessMode.READONLY);
        mlResults = WekaUtils.getFile(getContext(), "",
                WekaTestTask.evaluationBin, AccessMode.READONLY);

        Instances predictions = WekaUtils.getInstances(arff, isMultiLabel);

        List<String> labels = getLabels(predictions, isMultiLabel, isRegression);
        
        Properties props;
        
        if(isMultiLabel){
        	MultilabelResult r = WekaUtils.readMlResultFromFile(mlResults);
        	props = generateMlProperties(predictions, labels, r);
        }
        else{
        	Map<Integer, String> documentIdMap = loadDocumentMap();
        	props = generateSlProperties(predictions, isRegression, isUnit, documentIdMap, labels);
        }
        
        FileWriterWithEncoding fw = null;
        try{
			fw = new FileWriterWithEncoding(getTargetOutputFile(), "utf-8");
			props.store(fw, generateHeader(labels));
		}finally{
			IOUtils.closeQuietly(fw);
		}
    }
	
	protected File getTargetOutputFile() {
		File evaluationFolder = getContext().getFolder("", AccessMode.READWRITE);
		return new File(evaluationFolder, ID_OUTCOME_KEY);
	}

    private List<String> getLabels(Instances predictions, boolean multiLabel, boolean regression)
    {
        if (regression) {
            return Collections.emptyList();
        }

        return WekaUtils.getClassLabels(predictions, multiLabel);
    }

    protected static String generateHeader(List<String> labels)
        throws UnsupportedEncodingException
    {
        StringBuilder comment = new StringBuilder();
        comment.append("ID=PREDICTION" + SEPARATOR_CHAR + "GOLDSTANDARD" + SEPARATOR_CHAR
                + "THRESHOLD" + "\n" + "labels");

        // add numbered indexing of labels: e.g. 0=NPg, 1=JJ
        for (int i = 0; i < labels.size(); i++) {
            comment.append(
                    " " + String.valueOf(i) + "=" + URLEncoder.encode(labels.get(i), "UTF-8"));
        }
        return comment.toString();
    }

    protected static Properties generateMlProperties(Instances predictions,
            List<String> labels, MultilabelResult r)
                throws ClassNotFoundException, IOException
    {
        Properties props = new SortedKeyProperties();
        String[] classValues = new String[predictions.numClasses()];

        for (int i = 0; i < predictions.numClasses(); i++) {
            classValues[i] = predictions.classAttribute().value(i);
        }

        int attOffset = predictions.attribute(ID_FEATURE_NAME).index();

            Map<String, Integer> class2number = classNamesToMapping(labels);
            int[][] goldmatrix = r.getGoldstandard();
            double[][] predictionsmatrix = r.getPredictions();
            double bipartition = r.getBipartitionThreshold();

            for (int i = 0; i < goldmatrix.length; i++) {
                Double[] predList = new Double[labels.size()];
                Integer[] goldList = new Integer[labels.size()];
                for (int j = 0; j < goldmatrix[i].length; j++) {
                    int classNo = class2number.get(labels.get(j));
                    goldList[classNo] = goldmatrix[i][j];
                    predList[classNo] = predictionsmatrix[i][j];
                }
                String s = (StringUtils.join(predList, ",") + SEPARATOR_CHAR
                        + StringUtils.join(goldList, ",") + SEPARATOR_CHAR + bipartition);
                String stringValue = predictions.get(i).stringValue(attOffset);
                props.setProperty(stringValue, s);
            }
        return props;
    }
    
    
    protected Properties generateSlProperties(Instances predictions,
            boolean isRegression, boolean isUnit, Map<Integer,String> documentIdMap, List<String> labels)
                throws Exception
    {
    	
        Properties props = new SortedKeyProperties();
        String[] classValues = new String[predictions.numClasses()];

        for (int i = 0; i < predictions.numClasses(); i++) {
            classValues[i] = predictions.classAttribute().value(i);
        }

        int attOffset = predictions.attribute(ID_FEATURE_NAME).index(); 
        
        prepareBaseline();
   
        int idx=0;
        for (Instance inst : predictions) {
            Double gold;
            try {
                gold = new Double(inst.value(predictions.attribute(
                        CLASS_ATTRIBUTE_NAME + WekaUtils.COMPATIBLE_OUTCOME_CLASS)));
            }
            catch (NullPointerException e) {
                // if train and test data have not been balanced
                gold = new Double(
                        inst.value(predictions.attribute(CLASS_ATTRIBUTE_NAME)));
            }
            Attribute gsAtt = predictions.attribute(WekaTestTask.PREDICTION_CLASS_LABEL_NAME);
            Double prediction = new Double(inst.value(gsAtt));
            if (!isRegression) {
                Map<String, Integer> class2number = classNamesToMapping(labels);
//                Integer predictionAsNumber = class2number
//                        .get(gsAtt.value(prediction.intValue()));
                Integer goldAsNumber = class2number.get(classValues[gold.intValue()]);
                
                String stringValue = inst.stringValue(attOffset);
				if (!isUnit && documentIdMap != null) {
					stringValue = documentIdMap.get(idx++);
				}
                props.setProperty(stringValue, getPrediction(prediction, class2number, gsAtt)
                        + SEPARATOR_CHAR + goldAsNumber + SEPARATOR_CHAR + String.valueOf(-1));
            }
            else {
                // the outcome is numeric
                String stringValue = inst.stringValue(attOffset);
				if (documentIdMap != null) {
					stringValue = documentIdMap.get(idx++);
				}
                props.setProperty(stringValue, prediction + SEPARATOR_CHAR
                        + gold + SEPARATOR_CHAR + String.valueOf(0));
            }
        }
        return props;
    }

	protected String getPrediction(Double prediction, Map<String, Integer> class2number, Attribute gsAtt) {
		// is overwritten in baseline reports
		 return class2number
                 .get(gsAtt.value(prediction.intValue())).toString();
	}

	protected void prepareBaseline() throws Exception {
		// is overwritten in baseline reports
	}

	private static Map<String, Integer> classNamesToMapping(List<String> labels) {
		Map<String, Integer> mapping = new HashMap<String, Integer>();
        for (int i = 0; i < labels.size(); i++) {
            mapping.put(labels.get(i), i);
        }
        return mapping;
	}

	private Map<Integer, String> loadDocumentMap() throws IOException {
		
		Map<Integer, String> documentIdMap = new HashMap<>();
		
		File f = new File(getContext().getFolder(TEST_TASK_INPUT_KEY_TEST_DATA, AccessMode.READONLY),
				FILENAME_DOCUMENT_META_DATA_LOG);
		List<String> readLines = FileUtils.readLines(f, "utf-8");

		int idx = 0;
		for (String l : readLines) {
			if (l.startsWith("#")) {
				continue;
			}
			String[] split = l.split("\t");
			documentIdMap.put(idx, split[0]);
			idx++;
		}
		
		return documentIdMap;
	}
}