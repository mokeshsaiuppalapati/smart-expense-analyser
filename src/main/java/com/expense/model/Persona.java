// File: src/main/java/com/expense/model/Persona.java

package com.expense.model;

import java.util.List;
import java.util.Map;

public class Persona {

    private final String title;
    private final List<ClusterDescription> clusterDescriptions;

    public Persona(String title, List<ClusterDescription> clusterDescriptions) {
        this.title = title;
        this.clusterDescriptions = clusterDescriptions;
    }

    public String getTitle() {
        return title;
    }

    public List<ClusterDescription> getClusterDescriptions() {
        return clusterDescriptions;
    }

    /**
     * A nested class to hold the analysis of a single spending cluster.
     */
    public static class ClusterDescription {
        private final String name;
        private final int transactionCount;
        private final double averageAmount;
        private final String timeFocus; // e.g., "Weekdays", "Weekends"
        private final List<Map.Entry<String, Long>> topCategories;

        public ClusterDescription(String name, int count, double avgAmount, String timeFocus, List<Map.Entry<String, Long>> topCategories) {
            this.name = name;
            this.transactionCount = count;
            this.averageAmount = avgAmount;
            this.timeFocus = timeFocus;
            this.topCategories = topCategories;
        }

        public String getName() { return name; }
        public int getTransactionCount() { return transactionCount; }
        public double getAverageAmount() { return averageAmount; }
        public String getTimeFocus() { return timeFocus; }
        public List<Map.Entry<String, Long>> getTopCategories() { return topCategories; }
    }
}