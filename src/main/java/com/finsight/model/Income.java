package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Money coming in, such as salary, a scholarship or a refund. */
public class Income extends Transaction {

    private String source;

    public Income(long id, BigDecimal amount, LocalDate date, Category category, String description, String source) {
        super(id, amount, date, category, description);
        setSource(source);
    }

    public Income(BigDecimal amount, LocalDate date, Category category, String description) {
        this(0, amount, date, category, description, "");
    }

    @Override
    public TransactionType getType() {
        return TransactionType.INCOME;
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount();
    }

    /** Who paid, for example an employer name. Optional. */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? "" : source.strip();
    }
}
