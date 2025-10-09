package com.expense.ml;

public class TrainRunner {
    public static void main(String[] args) throws Exception {
        // MODIFIED: Call the trainAndSave method with the new corrections.csv argument
        new WekaTrainer().trainAndSave(
                "data/transactions_labeled.csv",
                "data/corrections.csv", // Added this argument
                "model/classifier.model",
                "model/filter.model",
                "model/header.instance"
        );
        System.out.println("Training done.");
    }
}