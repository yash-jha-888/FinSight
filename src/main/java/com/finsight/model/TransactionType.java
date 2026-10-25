package com.finsight.model;

/** Direction of money flow. Stored as text in the database. */
public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
