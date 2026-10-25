package com.finsight.dao;

import java.math.BigDecimal;

/** Converts between {@link BigDecimal} amounts and the whole-cent integers stored in the database. */
final class Money {

    private Money() {
    }

    static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
