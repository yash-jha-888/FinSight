package com.finsight.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single movement of money.
 *
 * <p><b>OOP: abstraction and inheritance.</b> You never create a plain
 * {@code Transaction}; you create an {@link Income} or an {@link Expense}.
 * Shared state and validation live here once, and each subclass decides what
 * its type is and which way it moves the balance.
 *
 * <p><b>OOP: encapsulation.</b> Amounts are stored as positive
 * {@link BigDecimal}s rounded to two places, so floating point errors never
 * creep into totals.
 */
public abstract class Transaction implements Identifiable, Comparable<Transaction> {

    private long id;
    private BigDecimal amount;
    private LocalDate date;
    private Category category;
    private String description;

    protected Transaction(long id, BigDecimal amount, LocalDate date, Category category, String description) {
        this.id = id;
        setAmount(amount);
        setDate(date);
        setCategory(category);
        setDescription(description);
    }

    /** Which kind of transaction this is. */
    public abstract TransactionType getType();

    /**
     * The amount with a sign that shows its effect on the balance.
     *
     * <p><b>OOP: polymorphism.</b> Summing {@code getSignedAmount()} over a
     * mixed list gives the net balance without any {@code instanceof} checks.
     */
    public abstract BigDecimal getSignedAmount();

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date, "date");
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        Objects.requireNonNull(category, "category");
        if (category.getType() != getType()) {
            throw new IllegalArgumentException(
                    "Category '" + category.getName() + "' is for " + category.getType().getLabel()
                            + ", not " + getType().getLabel());
        }
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.strip();
    }

    /** Newest first, then by id so the order is stable. */
    @Override
    public int compareTo(Transaction other) {
        int byDate = other.date.compareTo(date);
        return byDate != 0 ? byDate : Long.compare(other.id, id);
    }

    @Override
    public String toString() {
        return "%s %s %s on %s (%s)".formatted(getType().getLabel(), amount, category, date, description);
    }
}
