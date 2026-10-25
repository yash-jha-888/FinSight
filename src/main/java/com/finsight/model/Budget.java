package com.finsight.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Objects;

/**
 * A spending limit for one expense category in one month.
 *
 * <p>The budget only knows its limit. How much has been spent is calculated
 * from transactions and passed in, so there is a single source of truth.
 */
public class Budget implements Identifiable {

    private long id;
    private Category category;
    private YearMonth month;
    private BigDecimal limit;

    public Budget(long id, Category category, YearMonth month, BigDecimal limit) {
        this.id = id;
        setCategory(category);
        setMonth(month);
        setLimit(limit);
    }

    public Budget(Category category, YearMonth month, BigDecimal limit) {
        this(0, category, month, limit);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        Objects.requireNonNull(category, "category");
        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set on expense categories");
        }
        this.category = category;
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = Objects.requireNonNull(month, "month");
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        Objects.requireNonNull(limit, "limit");
        if (limit.signum() <= 0) {
            throw new IllegalArgumentException("Budget limit must be greater than zero");
        }
        this.limit = limit.setScale(2, RoundingMode.HALF_UP);
    }

    /** Fraction of the limit used, for example 0.85 for 85%. Can exceed 1. */
    public double usedFraction(BigDecimal spent) {
        return spent.divide(limit, 4, RoundingMode.HALF_UP).doubleValue();
    }

    /** What is left to spend. Negative when the budget is overspent. */
    public BigDecimal remaining(BigDecimal spent) {
        return limit.subtract(spent);
    }
}
