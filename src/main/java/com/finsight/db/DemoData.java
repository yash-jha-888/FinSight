package com.finsight.db;

import com.finsight.model.Expense;
import com.finsight.model.Income;

import java.time.LocalDate;

/** Adds some sample transactions the first time the app runs, so there is something to show. */
public class DemoData {

    public static void addIfEmpty(TransactionDao dao) {
        if (!dao.getAll().isEmpty()) {
            return; // the user already has data
        }
        dao.add(new Income(0, 60000, LocalDate.parse("2026-10-01"), "Salary", "Monthly salary"));
        dao.add(new Expense(0, 18000, LocalDate.parse("2026-10-02"), "Rent", "Flat rent", "UPI"));
        dao.add(new Expense(0, 2400, LocalDate.parse("2026-10-05"), "Food", "Groceries", "Card"));
        dao.add(new Expense(0, 600, LocalDate.parse("2026-10-08"), "Transport", "Metro card", "UPI"));
        dao.add(new Expense(0, 1800, LocalDate.parse("2026-10-10"), "Bills", "Electricity and internet", "UPI"));
        dao.add(new Expense(0, 350, LocalDate.parse("2026-10-12"), "Food", "Lunch", "Cash"));
        dao.add(new Expense(0, 800, LocalDate.parse("2026-10-15"), "Entertainment", "Movie", "Card"));
        dao.add(new Income(0, 8000, LocalDate.parse("2026-10-17"), "Freelance", "Logo design"));
        dao.add(new Expense(0, 2200, LocalDate.parse("2026-10-18"), "Shopping", "Clothes", "Card"));
        dao.add(new Expense(0, 2600, LocalDate.parse("2026-10-20"), "Food", "Groceries", "UPI"));
        dao.add(new Expense(0, 450, LocalDate.parse("2026-10-22"), "Transport", "Auto", "Cash"));
        dao.add(new Expense(0, 500, LocalDate.parse("2026-10-25"), "Health", "Pharmacy", "Cash"));
    }
}
