package com.expense.ml;

import com.expense.model.TransactionData;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import java.util.ArrayList;
import java.util.List;

public class ExpensePredictor {
    private IBk model;

    public void train(List<TransactionData> transactionData) throws Exception {
        if (transactionData == null || transactionData.isEmpty()) {
            throw new IllegalArgumentException("Training data cannot be empty.");
        }

        // Define the new set of attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("dayOfWeek"));
        attributes.add(new Attribute("dayOfMonth"));
        attributes.add(new Attribute("month"));
        attributes.add(new Attribute("amount"));
        attributes.add(new Attribute("categoryCode"));

        Instances data = new Instances("TransactionData", attributes, transactionData.size());
        data.setClassIndex(3); // 'amount' is the 4th attribute (index 3)

        // Populate the dataset with the new features
        for (TransactionData tx : transactionData) {
            Instance inst = new DenseInstance(5);
            inst.setValue(attributes.get(0), tx.dayOfWeek);
            inst.setValue(attributes.get(1), tx.dayOfMonth);
            inst.setValue(attributes.get(2), tx.month);
            inst.setValue(attributes.get(3), tx.amount);
            inst.setValue(attributes.get(4), tx.categoryCode);
            data.add(inst);
        }

        model = new IBk();
        model.buildClassifier(data);
    }

    /**
     * MODIFIED: predict method now takes more features
     */
    public double predict(double dayOfWeek, double dayOfMonth, double month, int categoryCode) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Model not trained. Call train() first.");
        }

        // Define the structure for a single instance with new features
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("dayOfWeek"));
        attributes.add(new Attribute("dayOfMonth"));
        attributes.add(new Attribute("month"));
        attributes.add(new Attribute("amount"));
        attributes.add(new Attribute("categoryCode"));

        Instances predictionDataset = new Instances("PredictionInstance", attributes, 1);
        predictionDataset.setClassIndex(3);

        // Create the instance with the new features
        Instance inst = new DenseInstance(5);
        inst.setDataset(predictionDataset);
        inst.setValue(attributes.get(0), dayOfWeek);
        inst.setValue(attributes.get(1), dayOfMonth);
        inst.setValue(attributes.get(2), month);
        inst.setMissing(attributes.get(3)); // Mark 'amount' as missing
        inst.setValue(attributes.get(4), categoryCode);

        return model.classifyInstance(inst);
    }
}