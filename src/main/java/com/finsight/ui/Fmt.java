package com.finsight.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting for money, percentages and dates, in one place so the whole app is consistent. */
public final class Fmt {

    private static final Locale INDIA = Locale.of("en", "IN");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private Fmt() {
    }

    /** ₹12,345.50 */
    public static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(INDIA).format(amount);
    }

    /** ₹12,346, no paise. */
    public static String moneyWhole(BigDecimal amount) {
        var f = NumberFormat.getCurrencyInstance(INDIA);
        f.setMaximumFractionDigits(0);
        return f.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    /** ₹12.3k, for chart axes. */
    public static String moneyCompact(BigDecimal amount) {
        double v = amount.doubleValue();
        if (Math.abs(v) >= 100_000) return "₹" + trim(v / 100_000) + "L";
        if (Math.abs(v) >= 1_000) return "₹" + trim(v / 1_000) + "k";
        return "₹" + (long) v;
    }

    /** +₹500.00 or −₹500.00, with a true minus sign. */
    public static String signed(BigDecimal amount) {
        String body = money(amount.abs());
        return (amount.signum() < 0 ? "−" : "+") + body;
    }

    public static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    public static String month(YearMonth m) {
        return MONTH.format(m);
    }

    public static String monthShort(YearMonth m) {
        return MONTH_SHORT.format(m);
    }

    public static String day(LocalDate d) {
        return DAY.format(d);
    }

    public static String weekday(LocalDate d) {
        return WEEKDAY.format(d);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
