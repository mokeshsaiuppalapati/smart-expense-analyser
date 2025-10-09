package com.expense.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.util.Map;

public class ChartHelper {

    public static void showPieChart(String title, Map<String, Double> data) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            dataset.setValue(e.getKey(), e.getValue());
        }
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        showInFrame(title, chart);
    }

    public static void showBarChart(String title, Map<String, Double> months) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> e : months.entrySet()) {
            dataset.addValue(e.getValue(), "Amount", e.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(title, "Month", "Amount", dataset);
        showInFrame(title, chart);
    }

    private static void showInFrame(String title, JFreeChart chart) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ChartPanel(chart));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

