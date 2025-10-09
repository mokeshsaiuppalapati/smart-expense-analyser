package com.expense.ml;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.core.SerializationHelper;

public class WekaPredictor {
    private Classifier classifier;
    private Filter filter;
    private Instances rawHeader;

    public WekaPredictor() {
        System.out.println("--- A new WekaPredictor instance has been created. ---");
        reloadModel();
    }

    public void reloadModel() {
        try {
            classifier = (Classifier) SerializationHelper.read("model/classifier.model");
            filter = (Filter) SerializationHelper.read("model/filter.model");
            rawHeader = (Instances) SerializationHelper.read("model/header.instance");
            System.out.println(">>> SUCCESS: reloadModel() completed successfully. The model is now loaded. <<<");
        } catch (Exception e) {
            System.err.println(">>> FAILURE: reloadModel() failed. The model is NOT loaded. See error below. <<<");
            e.printStackTrace();
            classifier = null;
            filter = null;
            rawHeader = null;
        }
    }

    public boolean isModelLoaded() {
        return classifier != null && filter != null && rawHeader != null;
    }

    public Result predict(String description) {
        try {
            if (!isModelLoaded()) return new Result("Other", 0.0);

            Instances inst = WekaHelper.makeInstanceFromRawHeader(rawHeader, description);
            Instances filtered = Filter.useFilter(inst, filter);

            double idx = classifier.classifyInstance(filtered.instance(0));
            double[] dist = classifier.distributionForInstance(filtered.instance(0));
            String category = filtered.classAttribute().value((int) idx);
            double conf = dist[(int) idx];
            return new Result(category, conf);
        } catch (Exception e) {
            e.printStackTrace();
            // MODIFIED: Instead of returning "Other", return the error message
            // This will make any hidden errors visible in the UI.
            String errorMessage = "Error: " + e.getMessage();
            return new Result(errorMessage, 0.0);
        }
    }

    public static class Result {
        public final String category;
        public final double confidence;
        public Result(String c, double conf) { category = c; confidence = conf; }
    }
}