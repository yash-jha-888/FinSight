package com.finsight.service;

import com.finsight.dao.BudgetRepository;
import com.finsight.dao.CategoryRepository;
import com.finsight.dao.Database;
import com.finsight.dao.TransactionRepository;
import com.finsight.model.Budget;
import com.finsight.model.Category;
import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.PaymentMethod;
import com.finsight.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fills an empty database with default categories and a few months of
 * realistic history, so the app has something to show on first launch.
 * The random seed is fixed, so every demo looks the same.
 */
public class DemoData {

    private final Database db;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;

    public DemoData(Database db, CategoryRepository categories, TransactionRepository transactions,
                    BudgetRepository budgets) {
        this.db = db;
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
    }

    /** Creates the default categories if there are none. Returns them by name. */
    public Map<String, Category> ensureDefaultCategories() {
        Map<String, Category> byName = new LinkedHashMap<>();
        if (!categories.findAll().isEmpty()) {
            categories.findAll().forEach(c -> byName.put(c.getName(), c));
            return byName;
        }
        String[][] defaults = {
                {"Salary", "INCOME", "#3F6E57"},
                {"Freelance", "INCOME", "#6F8F5E"},
                {"Other income", "INCOME", "#8DA399"},
                {"Rent", "EXPENSE", "#3C4A63"},
                {"Groceries", "EXPENSE", "#B4653E"},
                {"Dining out", "EXPENSE", "#C9A04A"},
                {"Transport", "EXPENSE", "#5D7A8C"},
                {"Utilities", "EXPENSE", "#7B6A8F"},
                {"Shopping", "EXPENSE", "#A8505A"},
                {"Entertainment", "EXPENSE", "#4F8A86"},
                {"Health", "EXPENSE", "#8C7B5D"},
        };
        for (String[] d : defaults) {
            var c = categories.save(new Category(d[0], TransactionType.valueOf(d[1]), d[2]));
            byName.put(c.getName(), c);
        }
        return byName;
    }

    /** Seeds {@code months} months of history ending today, if there are no transactions yet. */
    public void seedIfEmpty(LocalDate today, int months) {
        if (!transactions.isEmpty()) {
            return;
        }
        var c = ensureDefaultCategories();
        var rnd = new Random(42);
        YearMonth current = YearMonth.from(today);

        db.inTransaction(conn -> {
            for (int m = months - 1; m >= 0; m--) {
                YearMonth month = current.minusMonths(m);
                int lastDay = m == 0 ? today.getDayOfMonth() : month.lengthOfMonth();

                income(month.atDay(1), "62000", c.get("Salary"), "Monthly salary", "Northwind Labs");
                if (rnd.nextInt(3) > 0 && lastDay >= 15) {
                    income(month.atDay(15), amount(rnd, 4000, 12000), c.get("Freelance"),
                            pick(rnd, "Logo design", "Website fixes", "Tutoring"), "Client");
                }

                expense(month.atDay(Math.min(3, lastDay)), "18000", c.get("Rent"), "Flat rent", PaymentMethod.BANK_TRANSFER);
                if (lastDay >= 10) {
                    expense(month.atDay(10), amount(rnd, 1400, 2600), c.get("Utilities"),
                            "Electricity and internet", PaymentMethod.UPI);
                }

                for (int day = 1; day <= lastDay; day++) {
                    LocalDate date = month.atDay(day);
                    if (rnd.nextDouble() < 0.28) {
                        expense(date, amount(rnd, 300, 1800), c.get("Groceries"),
                                pick(rnd, "BigBasket order", "Vegetables", "Supermarket", "Milk and bread"),
                                PaymentMethod.UPI);
                    }
                    if (rnd.nextDouble() < 0.22) {
                        expense(date, amount(rnd, 180, 1500), c.get("Dining out"),
                                pick(rnd, "Lunch with friends", "Coffee", "Dinner", "Swiggy order"),
                                pick(rnd, PaymentMethod.UPI, PaymentMethod.CARD));
                    }
                    if (rnd.nextDouble() < 0.35) {
                        expense(date, amount(rnd, 40, 450), c.get("Transport"),
                                pick(rnd, "Metro", "Auto", "Uber", "Fuel"),
                                pick(rnd, PaymentMethod.UPI, PaymentMethod.CASH));
                    }
                    if (rnd.nextDouble() < 0.06) {
                        expense(date, amount(rnd, 800, 4500), c.get("Shopping"),
                                pick(rnd, "Clothes", "Headphones", "Books", "Home goods"), PaymentMethod.CARD);
                    }
                    if (rnd.nextDouble() < 0.07) {
                        expense(date, amount(rnd, 200, 1200), c.get("Entertainment"),
                                pick(rnd, "Movie tickets", "Concert", "Streaming", "Bowling"), PaymentMethod.CARD);
                    }
                    if (rnd.nextDouble() < 0.03) {
                        expense(date, amount(rnd, 300, 2500), c.get("Health"),
                                pick(rnd, "Pharmacy", "Doctor visit", "Gym"), PaymentMethod.UPI);
                    }
                }

                budget(c.get("Groceries"), month, "9000");
                budget(c.get("Dining out"), month, "5000");
                budget(c.get("Transport"), month, "3500");
                budget(c.get("Shopping"), month, "4000");
                budget(c.get("Entertainment"), month, "2500");
            }
            return null;
        });
    }

    private void income(LocalDate date, String amount, Category category, String description, String source) {
        transactions.save(new Income(0, new BigDecimal(amount), date, category, description, source));
    }

    private void expense(LocalDate date, String amount, Category category, String description, PaymentMethod method) {
        transactions.save(new Expense(new BigDecimal(amount), date, category, description, method));
    }

    private void budget(Category category, YearMonth month, String limit) {
        budgets.save(new Budget(category, month, new BigDecimal(limit)));
    }

    /** A random amount rounded to the nearest 10, as a string. */
    private static String amount(Random rnd, int min, int max) {
        return String.valueOf((min + rnd.nextInt(max - min + 1)) / 10 * 10);
    }

    @SafeVarargs
    private static <T> T pick(Random rnd, T... options) {
        return options[rnd.nextInt(options.length)];
    }
}
