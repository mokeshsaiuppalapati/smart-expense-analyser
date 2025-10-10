package com.expense.util;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

public class CategoryIconManager {

    public static FontIcon getIcon(String category) {
        if (category == null) {
            return new FontIcon(MaterialDesignA.ALERT_CIRCLE_OUTLINE);
        }

        return switch (category.toLowerCase()) {
            case "food", "food and drinks" -> new FontIcon(MaterialDesignF.FOOD);
            case "shopping" -> new FontIcon(MaterialDesignS.SHOPPING);
            case "housing" -> new FontIcon(MaterialDesignH.HOME);
            case "transport", "transportation" -> new FontIcon(MaterialDesignB.BUS);
            case "vehicle" -> new FontIcon(MaterialDesignC.CAR);
            case "entertainment" -> new FontIcon(MaterialDesignB.BASKETBALL);
            case "groceries", "grocery" -> new FontIcon(MaterialDesignC.CART);
            default -> new FontIcon(MaterialDesignA.ALERT_CIRCLE_OUTLINE);
        };
    }
}