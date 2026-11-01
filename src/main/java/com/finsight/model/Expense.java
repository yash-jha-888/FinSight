package com.finsight.model;

import java.time.LocalDate;

/** Money going out. Adds one extra field: how it was paid. */
public class Expense extends Transaction {

    private String paymentMethod;

    public Expense(int id, double amount, LocalDate date, String category, String description,
                   String paymentMethod) {
        super(id, amount, date, category, description);
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String getType() {
        return "Expense";
    }

    @Override
    public double getSignedAmount() {
        return -getAmount();
    }

    @Override
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
