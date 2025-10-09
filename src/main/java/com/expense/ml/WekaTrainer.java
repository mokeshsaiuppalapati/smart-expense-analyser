package com.expense.ml;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToString; // ADDED
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.File;
import java.util.Random;

public class WekaTrainer {

    public void trainAndSave(String baseCsvPath, String correctionsCsvPath, String modelPath, String filterPath, String headerPath) throws Exception {

        // 1. Load Datasets
        DataSource baseSource = new DataSource(baseCsvPath);
        Instances baseData = baseSource.getDataSet();

        File correctionsFile = new File(correctionsCsvPath);
        if (correctionsFile.exists()) {
            DataSource correctionsSource = new DataSource(correctionsCsvPath);
            Instances correctionsData = correctionsSource.getDataSet();
            if (correctionsData.numInstances() > 0) {
                for (int i = 0; i < correctionsData.numInstances(); i++) {
                    baseData.add(correctionsData.instance(i));
                }
                System.out.println("Merged " + correctionsData.numInstances() + " user corrections.");
            }
        }

        baseData.setClassIndex(baseData.numAttributes() - 1);

        // --- THIS IS THE NEW FIX ---
        // 2. Force the first attribute (description) to be a String type.
        // This prevents Weka from incorrectly guessing it's a fixed list (Nominal).
        if (baseData.attribute(0).isNominal()) {
            System.out.println("Description attribute was nominal. Converting to String.");
            NominalToString nominalToStringFilter = new NominalToString();
            nominalToStringFilter.setAttributeIndexes("1"); // Attribute index is 1-based
            nominalToStringFilter.setInputFormat(baseData);
            baseData = Filter.useFilter(baseData, nominalToStringFilter);
        }
        // --- END OF FIX ---

        // 3. Apply the StringToWordVector filter (same as before)
        StringToWordVector filter = new StringToWordVector();
        filter.setLowerCaseTokens(true);
        filter.setInputFormat(baseData);
        Instances filteredData = Filter.useFilter(baseData, filter);

        // 4. Train the classifier
        Classifier cls = new NaiveBayes();
        cls.buildClassifier(filteredData);

        // 5. Evaluate and save
        Evaluation eval = new Evaluation(filteredData);
        eval.crossValidateModel(cls, filteredData, Math.min(10, filteredData.numInstances()), new Random(1));
        System.out.println(eval.toSummaryString());

        new File("model").mkdirs();
        SerializationHelper.write(modelPath, cls);
        SerializationHelper.write(filterPath, filter);
        // Save the raw, unfiltered header (which now correctly has a String attribute)
        SerializationHelper.write(headerPath, new Instances(baseData, 0));
    }
}