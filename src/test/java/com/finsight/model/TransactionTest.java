package com.finsight.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private final Category salary = new Category(1, "Salary", TransactionType.INCOME, "#2F6B4F");
    private final Category food = new Category(2, "Food", TransactionType.EXPENSE, "#B5523B");
    private final LocalDate day = LocalDate.of(2026, 10, 1);

    @Test
    void signedAmountsSumToNetBalance() {
        List<Transaction> list = List.of(
                new Income(new BigDecimal("50000"), day, salary, "October pay"),
                new Expense(new BigDecimal("1200.50"), day, food, "Groceries", PaymentMethod.UPI),
                new Expense(new BigDecimal("300"), day, food, "Lunch", PaymentMethod.CASH));

        BigDecimal net = list.stream().map(Transaction::getSignedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("48499.50"), net);
    }

    @Test
    void amountIsRoundedToTwoPlaces() {
        var e = new Expense(new BigDecimal("10.005"), day, food, "", PaymentMethod.CARD);
        assertEquals(new BigDecimal("10.01"), e.getAmount());
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense(BigDecimal.ZERO, day, food, "", PaymentMethod.CASH));
        assertThrows(IllegalArgumentException.class,
                () -> new Income(new BigDecimal("-5"), day, salary, ""));
    }

    @Test
    void rejectsCategoryOfTheWrongType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense(BigDecimal.TEN, day, salary, "", PaymentMethod.CASH));
    }

    @Test
    void sortsNewestFirst() {
        var older = new Expense(1, BigDecimal.ONE, day, food, "", PaymentMethod.CASH);
        var newer = new Expense(2, BigDecimal.ONE, day.plusDays(3), food, "", PaymentMethod.CASH);
        assertEquals(List.of(newer, older), List.of(older, newer).stream().sorted().toList());
    }

    @Test
    void categoryValidatesColour() {
        assertThrows(IllegalArgumentException.class, () -> new Category("Rent", TransactionType.EXPENSE, "red"));
        assertEquals("#ABCDEF", new Category("Rent", TransactionType.EXPENSE, "#abcdef").getColorHex());
    }
}
