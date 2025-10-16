// File: src/main/java/com/expense/ml/WekaPredictor.java

package com.expense.ml;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WekaPredictor {
    private Classifier classifier;
    private Filter filter;
    private Instances rawHeader;

    public WekaPredictor() {
        reloadModel();
    }

    public void reloadModel() {
        try {
            classifier = (Classifier) SerializationHelper.read("model/classifier.model");
            filter = (Filter) SerializationHelper.read("model/filter.model");
            rawHeader = (Instances) SerializationHelper.read("model/header.instance");
            System.out.println(">>> SUCCESS: WekaPredictor model reloaded.");
        } catch (Exception e) {
            System.err.println(">>> FAILURE: WekaPredictor model failed to load.");
            classifier = null;
        }
    }

    public boolean isModelLoaded() {
        return classifier != null && filter != null && rawHeader != null;
    }

    public Result predict(String description) {
        if (!isModelLoaded()) return new Result("Other", 0.0);
        try {
            Instances inst = WekaHelper.makeInstanceFromRawHeader(rawHeader, description);
            Instances filtered = Filter.useFilter(inst, filter);
            double[] dist = classifier.distributionForInstance(filtered.instance(0));
            double idx = 0;
            double maxConf = -1;
            for(int i = 0; i < dist.length; i++) {
                if(dist[i] > maxConf) {
                    maxConf = dist[i];
                    idx = i;
                }
            }
            String category = filtered.classAttribute().value((int) idx);
            return new Result(category, maxConf);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result("Error", 0.0);
        }
    }

    // --- NEW METHOD FOR SMART SPLIT ---
    public List<String> predictTopCategories(String description) {
        List<CategoryPrediction> predictions = new ArrayList<>();
        if (!isModelLoaded()) return new ArrayList<>();

        try {
            Instances inst = WekaHelper.makeInstanceFromRawHeader(rawHeader, description);
            Instances filtered = Filter.useFilter(inst, filter);
            double[] distribution = classifier.distributionForInstance(filtered.instance(0));

            for (int i = 0; i < distribution.length; i++) {
                // Only consider categories with at least 5% confidence
                if (distribution[i] > 0.05) {
                    String category = filtered.classAttribute().value(i);
                    predictions.add(new CategoryPrediction(category, distribution[i]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        // Sort by confidence and return the top 3 category names
        return predictions.stream()
                .sorted(Comparator.comparingDouble(CategoryPrediction::getConfidence).reversed())
                .limit(3)
                .map(CategoryPrediction::getCategory)
                .collect(Collectors.toList());
    }

    // Helper class for sorting predictions
    private static class CategoryPrediction {
        private final String category;
        private final double confidence;
        public CategoryPrediction(String category, double confidence) {
            this.category = category;
            this.confidence = confidence;
        }
        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
    }

    // Public result class for single predictions
    public static class Result {
        public final String category;
        public final double confidence;
        public Result(String c, double conf) { category = c; confidence = conf; }
    }
}