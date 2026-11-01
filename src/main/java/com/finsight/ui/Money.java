package com.finsight.ui;

/** Formats numbers as rupees, for example 1250.5 becomes "₹1,250.50" and -300 becomes "-₹300.00". */
public class Money {

    public static String format(double amount) {
        if (amount < 0) {
            return "-₹" + String.format("%,.2f", -amount);
        }
        return "₹" + String.format("%,.2f", amount);
    }
}
