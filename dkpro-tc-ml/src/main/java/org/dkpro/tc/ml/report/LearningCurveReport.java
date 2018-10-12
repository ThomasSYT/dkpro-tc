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
package org.dkpro.tc.ml.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.dkpro.lab.reporting.ChartUtil;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.lab.task.impl.TaskBase;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.TcTaskTypeUtil;
import org.dkpro.tc.core.util.ReportUtils;
import org.dkpro.tc.ml.report.util.Tc2LtlabEvalConverter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import de.unidue.ltl.evaluation.core.EvaluationData;
import de.unidue.ltl.evaluation.measures.Accuracy;
import de.unidue.ltl.evaluation.measures.EvaluationMeasure;
import de.unidue.ltl.evaluation.measures.categorial.Fscore;
import de.unidue.ltl.evaluation.measures.categorial.Precision;
import de.unidue.ltl.evaluation.measures.categorial.Recall;
import de.unidue.ltl.evaluation.measures.correlation.PearsonCorrelation;
import de.unidue.ltl.evaluation.measures.correlation.SpearmanCorrelation;

/**
 * Collects the final evaluation results in a cross validation setting.
 * 
 */
public class LearningCurveReport
    extends TcAbstractReport
    implements Constants
{
    int maxNumberFolds=-1;
    
    @Override
    public void execute() throws Exception
    {
        StorageService store = getContext().getStorageService();
        Set<String> idPool = getTaskIdsFromMetaData(getSubtasks());
        String learningMode = determineLearningMode(store, idPool);

        Map<RunIdentifier, Map<Integer, List<File>>> dataMap = writeOverallResults(learningMode,
                store, idPool);

        if (isSingleLabelMode(learningMode)) {
            writeCategoricalResults(learningMode, store, dataMap);
        }

    }

    @SuppressWarnings("rawtypes")
    private Map<RunIdentifier, Map<Integer, List<File>>> writeOverallResults(String learningMode,
            StorageService store, Set<String> idPool)
        throws Exception
    {

        Map<RunIdentifier, Map<Integer, List<File>>> dataMap = new HashMap<>();

        Set<String> collectSubtasks = null;
        for (String id : idPool) {

            if (!TcTaskTypeUtil.isCrossValidationTask(store, id)) {
                continue;
            }
            collectSubtasks = collectSubtasks(id);

            Map<RunIdentifier, Map<Integer, List<File>>> run = collectRuns(store, collectSubtasks);
            dataMap.putAll(run);
        }

        if (learningMode.equals(LM_SINGLE_LABEL)) {
            for (RunIdentifier configId : dataMap.keySet()) {
                List<Double> stageAveraged = averagePerStage(dataMap.get(configId), Accuracy.class);
                writePlot(configId.md5, stageAveraged, maxNumberFolds,
                        Accuracy.class.getSimpleName());
            }

        }
        else if (learningMode.equals(LM_REGRESSION)) {
            List<Class<? extends EvaluationMeasure>> regMetrics = new ArrayList<>();
            regMetrics.add(PearsonCorrelation.class);
            regMetrics.add(SpearmanCorrelation.class);
            for (Class<? extends EvaluationMeasure> m : regMetrics) {

                for (RunIdentifier configId : dataMap.keySet()) {
                    List<Double> stageAveraged = averagePerStage(dataMap.get(configId), m);
                    writePlot(configId.md5, stageAveraged, maxNumberFolds,
                            m.getSimpleName());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (RunIdentifier configId : dataMap.keySet()) {
            sb.append(configId.md5 + "\t" + configId.classification + configId.featureset + "\n");
        }

        FileUtils.writeStringToFile(getContext().getFile("md5Mapping.txt", AccessMode.READWRITE),
                sb.toString(), "utf-8");

        return dataMap;
    }

    @SuppressWarnings("rawtypes")
    private List<Double> averagePerStage(Map<Integer, List<File>> map,
            Class<? extends EvaluationMeasure> class1)
        throws Exception
    {
        List<Double> stageAveraged = new ArrayList<>();

        List<Integer> keys = new ArrayList<Integer>(map.keySet());
        Collections.sort(keys);
        for (Integer numFolds : keys) {
            List<File> st = map.get(numFolds);
            EvaluationData<String> stageData = new EvaluationData<>();
            for (File f : st) {
                EvaluationData<String> run = Tc2LtlabEvalConverter
                        .convertSingleLabelModeId2Outcome(f);
                stageData.registerBulk(run);
            }
            EvaluationMeasure measure = class1.getDeclaredConstructor(EvaluationData.class)
                    .newInstance(stageData);
            stageAveraged.add(measure.getResult());
        }
        return stageAveraged;
    }

    private Map<RunIdentifier, Map<Integer, List<File>>> collectRuns(StorageService store,
            Set<String> collectSubtasks)
        throws Exception
    {
        Map<RunIdentifier, Map<Integer, List<File>>> dataMap = new HashMap<>();
        List<String> sortedTasks = new ArrayList<>(collectSubtasks);
        Collections.sort(sortedTasks);
        for (String sId : sortedTasks) {
            if (!TcTaskTypeUtil.isMachineLearningAdapterTask(store, sId)) {
                continue;
            }

            int numberOfTrainFolds = getNumberOfTrainingFolds(store, sId);
            if(numberOfTrainFolds > maxNumberFolds) {
                maxNumberFolds = numberOfTrainFolds;
            }

            RunIdentifier configurationId = generateId(store, sId);
            Map<Integer, List<File>> idRun = dataMap.get(configurationId);

            if (idRun == null) {
                idRun = new HashMap<>();
            }

            List<File> stage = idRun.get(numberOfTrainFolds);
            if (stage == null) {
                stage = new ArrayList<>();
            }
            File f = store.locateKey(sId, ID_OUTCOME_KEY);
            stage.add(f);
            idRun.put(numberOfTrainFolds, stage);
            dataMap.put(configurationId, idRun);
        }
        return dataMap;
    }

    private RunIdentifier generateId(StorageService store, String sId) throws Exception
    {
        Properties p = new Properties();
        File locateKey = store.locateKey(sId, TaskBase.DISCRIMINATORS_KEY);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(locateKey);
            p.load(fis);
        }
        finally {
            IOUtils.closeQuietly(fis);
        }

        Map<String, String> m = new HashMap<>();
        for (Entry<Object, Object> e : p.entrySet()) {
            m.put(e.getKey().toString(), e.getValue().toString());
        }

        m = ReportUtils.removeKeyRedundancy(m);

        String classification = m.get(DIM_CLASSIFICATION_ARGS);
        String featureSet = m.get(DIM_FEATURE_SET);

        return new RunIdentifier(classification, featureSet);
    }

    private int getNumberOfTrainingFolds(StorageService store, String sId) throws Exception
    {

        Properties p = new Properties();
        File locateKey = store.locateKey(sId, CONFIGURATION_DKPRO_LAB);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(locateKey);
            p.load(fis);
            String foldValue = p.getProperty(DIM_NUM_TRAINING_FOLDS);

            if (foldValue == null) {
                throw new IllegalArgumentException(
                        "Retrieved null when retrieving the discriminator ["
                                + DIM_NUM_TRAINING_FOLDS + "]");
            }

            if (foldValue.contains(",")) {
                String[] split = foldValue.split(",");
                return split.length;
            }

        }
        finally {
            IOUtils.closeQuietly(fis);
        }

        return 1;
    }

    private void writePlot(String id, List<Double> stageAveraged, int maxFolds, String metricName)
        throws Exception
    {
        double x[] = new double[stageAveraged.size() + 1];
        double y[] = new double[stageAveraged.size() + 1];
        for (int i = 1; i < x.length; i++) {
            x[i] = i;
            y[i] = stageAveraged.get(i - 1);
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        double[][] data = new double[2][x.length];
        data[0] = x;
        data[1] = y;
        dataset.addSeries(metricName, data);

        JFreeChart chart = ChartFactory.createXYLineChart("Learning Curve",
                "number of training folds", metricName, dataset, PlotOrientation.VERTICAL, true,
                false, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        plot.setRenderer(renderer);
        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setRange(0.00, maxFolds);
        domain.setTickUnit(new NumberTickUnit(1.0));
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0.0, 1.0);
        range.setTickUnit(new NumberTickUnit(0.1));

        File file = getContext().getFile(id + "_learningCurve_" + metricName + ".pdf",
                AccessMode.READWRITE);
        FileOutputStream fos = new FileOutputStream(file);
        ChartUtil.writeChartAsPDF(fos, chart, 400, 400);
        fos.close();
    }

    private boolean isSingleLabelMode(String learningMode)
    {
        return learningMode.equals(Constants.LM_SINGLE_LABEL);
    }

    private void writeCategoricalResults(String learningMode, StorageService store,
            Map<RunIdentifier, Map<Integer, List<File>>> dataMap)
        throws Exception
    {

        for (RunIdentifier configId : dataMap.keySet()) {
            Map<Integer, List<File>> map = dataMap.get(configId);
            List<List<CategoricalPerformance>> stageAvg = averagePerStageCategorical(
                    map);
            writeCategoricalPlots(configId.md5, stageAvg, maxNumberFolds);
        }

    }

    private void writeCategoricalPlots(String md5, List<List<CategoricalPerformance>> allData,
            int maxValue)
        throws Exception
    {

        List<List<CategoricalPerformance>> allDataNormalized = ensureThatEachCategoryOccursAtEachStage(
                allData);

        for (int i = 0; i < allDataNormalized.get(0).size(); i++) {
            for (int j = 0; j < allData.size(); j++) {

                DefaultXYDataset dataset = new DefaultXYDataset();
                for (String key : new String[] { CategoricalPerformance.PRECISION,
                        CategoricalPerformance.RECALL, CategoricalPerformance.FSCORE }) {
                    double x[] = new double[allData.size() + 1];
                    double y[] = new double[allData.size() + 1];

                    int idx = 1;
                    for (List<CategoricalPerformance> currentStage : allDataNormalized) {
                        x[idx] = idx;
                        y[idx] = currentStage.get(i).getValue(key);
                        idx++;
                    }

                    double[][] data = new double[2][x.length];
                    data[0] = x;
                    data[1] = y;
                    dataset.addSeries(key, data);
                }

                JFreeChart chart = ChartFactory.createXYLineChart("CategoricalCurve",
                        "number of training folds", "Performance", dataset,
                        PlotOrientation.VERTICAL, true, false, false);
                XYPlot plot = (XYPlot) chart.getPlot();
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
                renderer.setSeriesLinesVisible(0, true);
                renderer.setSeriesShapesVisible(0, false);
                plot.setRenderer(renderer);
                NumberAxis domain = (NumberAxis) plot.getDomainAxis();
                domain.setRange(0.00, maxValue);
                domain.setTickUnit(new NumberTickUnit(1.0));
                NumberAxis range = (NumberAxis) plot.getRangeAxis();
                range.setRange(0.0, 1.0);
                range.setTickUnit(new NumberTickUnit(0.1));

                File file = getContext().getFile(md5 + "_categorical_"
                        + allDataNormalized.get(j).get(i).categoryName + ".pdf",
                        AccessMode.READWRITE);
                FileOutputStream fos = new FileOutputStream(file);
                ChartUtil.writeChartAsPDF(fos, chart, 400, 400);
                fos.close();

            }
        }
    }

    /**
     * It might happen that some categories did not occur in the training set depending on the data
     * distribution and how they were assigned to folds. We check this condition and add
     * zero-entries to ensure that on each learning curve stage all categories occur.
     */
    private List<List<CategoricalPerformance>> ensureThatEachCategoryOccursAtEachStage(
            List<List<CategoricalPerformance>> allData)
    {

        Set<String> setOfNames = new HashSet<>();
        for (List<CategoricalPerformance> cp : allData) {
            for (CategoricalPerformance c : cp) {
                setOfNames.add(c.categoryName);
            }
        }
        List<String> catogryNames = new ArrayList<>(setOfNames);
        Collections.sort(catogryNames);

        boolean needNormalization = false;
        for (List<CategoricalPerformance> stage : allData) {
            if (stage.size() != catogryNames.size()) {
                needNormalization = true;
                break;
            }
        }

        if (!needNormalization) {
            return allData;
        }

        List<List<CategoricalPerformance>> allDataNormalized = new ArrayList<>();

        for (List<CategoricalPerformance> stage : allData) {
            List<CategoricalPerformance> stageNormalized = new ArrayList<>();
            for (String name : catogryNames) {
                boolean added = false;
                for (CategoricalPerformance p : stage) {
                    if (p.categoryName.equals(name)) {
                        stageNormalized.add(p);
                        added = true;
                        break;
                    }
                    if (added) {
                        break;
                    }
                }
                if (!added) {
                    stageNormalized.add(new CategoricalPerformance(name, 0, 0, 0));
                }
            }
            allDataNormalized.add(stageNormalized);
        }

        return allDataNormalized;
    }

    private List<List<CategoricalPerformance>> averagePerStageCategorical(
            Map<Integer, List<File>> map)
        throws Exception
    {
        List<List<CategoricalPerformance>> stageAveraged = new ArrayList<>();

        List<Integer> keys = new ArrayList<Integer>(map.keySet());
        Collections.sort(keys);
        for (Integer numFolds : keys) {
            List<File> st = map.get(numFolds);
            EvaluationData<String> stageData = new EvaluationData<>();
            for (File f : st) {
                EvaluationData<String> run = Tc2LtlabEvalConverter
                        .convertSingleLabelModeId2Outcome(f);
                stageData.registerBulk(run);
            }

            Set<String> categories = new HashSet<>();
            stageData.forEach(x -> {
                categories.add(x.getGold());
                categories.add(x.getPredicted());
            });

            Precision<String> precision = new Precision<>(stageData);
            Recall<String> recall = new Recall<>(stageData);
            Fscore<String> fscore = new Fscore<>(stageData);
            List<CategoricalPerformance> cp = new ArrayList<>();
            for (String c : categories) {
                cp.add(new CategoricalPerformance(c, precision.getPrecisionForLabel(c),
                        recall.getRecallForLabel(c), fscore.getScoreForLabel(c)));
            }

            Collections.sort(cp, new Comparator<CategoricalPerformance>()
            {

                @Override
                public int compare(CategoricalPerformance o1, CategoricalPerformance o2)
                {
                    return o1.categoryName.compareTo(o2.categoryName);
                }
            });

            stageAveraged.add(cp);
        }
        return stageAveraged;
    }

    private String determineLearningMode(StorageService store, Set<String> idPool) throws Exception
    {
        String learningMode = getDiscriminator(store, idPool, DIM_LEARNING_MODE);
        if (learningMode == null) {
            for (String id : idPool) {
                Set<String> collectSubtasks = collectSubtasks(id);
                learningMode = getDiscriminator(store, collectSubtasks, DIM_LEARNING_MODE);
                if (learningMode != null) {
                    break;
                }
            }
        }
        return learningMode;
    }

    class RunIdentifier
    {
        String md5;
        String classification;
        String featureset;

        public RunIdentifier(String classification, String featureset)
            throws NoSuchAlgorithmException
        {
            this.classification = classification;
            this.featureset = featureset;

            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest((classification + featureset).getBytes());
            BigInteger bigInt = new BigInteger(1, digest);
            String md5 = bigInt.toString(16);
            this.md5 = md5;
        }

        @Override
        public int hashCode()
        {
            int result = 17;
            result = 31 * result + md5.hashCode();
            return result;
        }

        public boolean equals(Object other)
        {
            if (other == null)
                return false;
            if (other == this)
                return true;
            if (!(other instanceof RunIdentifier))
                return false;
            RunIdentifier otherMyClass = (RunIdentifier) other;
            if (otherMyClass.md5.equals(md5)) {
                return true;
            }
            return false;
        }
    }

    class CategoricalPerformance
    {
        static final String PRECISION = "PRECISION";
        static final String RECALL = "RECALL";
        static final String FSCORE = "FSCORE";
        String categoryName;
        double precision;
        double recall;
        double fscore;

        public CategoricalPerformance(String categoryName, double precision, double recall,
                double fscore)
        {
            this.categoryName = categoryName;
            this.precision = precision;
            this.recall = recall;
            this.fscore = fscore;
        }

        double getValue(String key)
        {
            switch (key) {
            case PRECISION:
                return precision;
            case RECALL:
                return recall;
            case FSCORE:
                return fscore;
            }

            throw new IllegalArgumentException(
                    "The key [" + key + "] is unknown as categorical measure");
        }

        public String toString()
        {
            return String.format("%s/%.3f/%.3f/%.3f", categoryName, precision, recall, fscore);
        }
    }
}
