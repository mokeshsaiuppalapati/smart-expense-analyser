package com.expense.ml;

public class AutoCategorizer {
    public static String predict(String desc) {
        if (desc == null) return "Other";
        String s = desc.toLowerCase();

        if (s.contains("pizza") || s.contains("restaurant") || s.contains("swiggy") || s.contains("domino"))
            return "Food";

        if (s.contains("petrol") || s.contains("fuel") || s.contains("uber") || s.contains("ola") || s.contains("bus"))
            return "Transport";

        if (s.contains("pharmacy") || s.contains("hospital") || s.contains("doctor"))
            return "Health";

        if (s.contains("electricity") || s.contains("bill") || s.contains("water") || s.contains("internet"))
            return "Bills";

        if (s.contains("grocery") || s.contains("supermarket") || s.contains("bazaar"))
            return "Groceries";

        if (s.contains("fee") || s.contains("tuition") || s.contains("school") || s.contains("college"))
            return "Fees";

        if (s.contains("shirt") || s.contains("pant") || s.contains("clothes"))
            return "Clothes";

        return "Other";
    }
}
