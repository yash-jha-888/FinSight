package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Money going out. Adds a payment method on top of the shared fields. */
public class Expense extends Transaction {

    private PaymentMethod paymentMethod;

    public Expense(long id, BigDecimal amount, LocalDate date, Category category, String description,
                   PaymentMethod paymentMethod) {
        super(id, amount, date, category, description);
        setPaymentMethod(paymentMethod);
    }

    public Expense(BigDecimal amount, LocalDate date, Category category, String description,
                   PaymentMethod paymentMethod) {
        this(0, amount, date, category, description, paymentMethod);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.EXPENSE;
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount().negate();
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod");
    }
}
