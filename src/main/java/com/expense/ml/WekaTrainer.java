// File: src/main/java/com/expense/ml/WekaTrainer.java

package com.expense.ml;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WekaTrainer {

    /**
     * This is the new, robust CSV loading method that uses Apache Commons CSV.
     * It correctly handles commas and quotes in descriptions.
     *
     * @param baseCsvPath       Path to the main training data.
     * @param correctionsCsvPath Path to the user corrections file.
     * @return An Instances object ready for training.
     * @throws IOException if files cannot be read.
     */
    private Instances loadDataRobustly(String baseCsvPath, String correctionsCsvPath) throws IOException {
        Set<String> allCategories = new HashSet<>();
        List<CSVRecord> allRecords = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();

        // --- Step 1: Read all data from both files to collect records and categories ---
        // Read base data
        try (Reader reader = new FileReader(baseCsvPath);
             CSVParser parser = new CSVParser(reader, csvFormat)) {
            for (CSVRecord record : parser) {
                allRecords.add(record);
                allCategories.add(record.get("Category"));
            }
        }

        // Read corrections data if it exists
        File correctionsFile = new File(correctionsCsvPath);
        if (correctionsFile.exists()) {
            try (Reader reader = new FileReader(correctionsCsvPath);
                 CSVParser parser = new CSVParser(reader, csvFormat)) {
                for (CSVRecord record : parser) {
                    allRecords.add(record);
                    allCategories.add(record.get(1)); // Corrections file might not have a header
                }
            }
        }

        // --- Step 2: Define the structure (Attributes) for Weka ---
        ArrayList<Attribute> attributes = new ArrayList<>();
        // Description is a String attribute
        attributes.add(new Attribute("Description", (List<String>) null));
        // Category is a Nominal attribute with all possible values we found
        attributes.add(new Attribute("Category", new ArrayList<>(allCategories)));

        // --- Step 3: Create the Instances object and populate it ---
        Instances data = new Instances("Transactions", attributes, allRecords.size());
        data.setClassIndex(1); // The "Category" attribute is our target

        for (CSVRecord record : allRecords) {
            double[] values = new double[2];
            // For the String attribute, we add its value to the attribute definition and get its index
            values[0] = data.attribute(0).addStringValue(record.get(0));
            // For the Nominal attribute, we get the index of its value
            values[1] = data.attribute(1).indexOfValue(record.get(1));
            data.add(new DenseInstance(1.0, values));
        }

        return data;
    }

    public void trainAndSave(String baseCsvPath, String correctionsCsvPath, String modelPath, String filterPath, String headerPath) throws Exception {

        // 1. Load data using our new robust method
        System.out.println("Loading data robustly with Apache Commons CSV...");
        Instances baseData = loadDataRobustly(baseCsvPath, correctionsCsvPath);
        System.out.println("Loaded " + baseData.numInstances() + " total instances.");

        // 2. Apply the StringToWordVector filter
        StringToWordVector filter = new StringToWordVector();
        filter.setLowerCaseTokens(true);
        filter.setInputFormat(baseData);
        Instances filteredData = Filter.useFilter(baseData, filter);

        // 3. Train the classifier
        Classifier cls = new NaiveBayes();
        cls.buildClassifier(filteredData);

        // 4. Evaluate and save
        Evaluation eval = new Evaluation(filteredData);
        int folds = Math.min(10, filteredData.numInstances());
        if (folds > 1) { // Cross-validation requires at least 2 instances
            eval.crossValidateModel(cls, filteredData, folds, new Random(1));
            System.out.println("Model Evaluation Summary:");
            System.out.println(eval.toSummaryString());
        }

        // 5. Save the trained models and headers
        new File("model").mkdirs();
        SerializationHelper.write(modelPath, cls);
        SerializationHelper.write(filterPath, filter);
        SerializationHelper.write(headerPath, new Instances(baseData, 0)); // Save the raw, unfiltered header
        System.out.println("Models saved successfully.");
    }
}