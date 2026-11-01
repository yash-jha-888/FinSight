package com.finsight.model;

import java.time.LocalDate;

/**
 * A single money transaction. This class is abstract, so we never create a
 * plain Transaction - we create an Income or an Expense instead.
 */
public abstract class Transaction {

    // private fields: other classes must use the getters and setters (encapsulation)
    private int id;
    private double amount;
    private LocalDate date;
    private String category;
    private String description;

    public Transaction(int id, double amount, LocalDate date, String category, String description) {
        this.id = id;
        setAmount(amount);
        this.date = date;
        this.category = category;
        this.description = description;
    }

    // Each subclass must say what type it is: "Income" or "Expense"
    public abstract String getType();

    // Income returns +amount, Expense returns -amount (polymorphism)
    public abstract double getSignedAmount();

    // Only expenses have a payment method, so here it is empty.
    // Expense overrides this method to return the real one.
    public String getPaymentMethod() {
        return "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be more than zero");
        }
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
