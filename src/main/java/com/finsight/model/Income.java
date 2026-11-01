package com.finsight.model;

import java.time.LocalDate;

/** Money coming in, like salary. Inherits everything from Transaction. */
public class Income extends Transaction {

    public Income(int id, double amount, LocalDate date, String category, String description) {
        super(id, amount, date, category, description);
    }

    @Override
    public String getType() {
        return "Income";
    }

    @Override
    public double getSignedAmount() {
        return getAmount();
    }
}
