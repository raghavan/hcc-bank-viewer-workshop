package moneywrapped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Week 3: Money Wrapped facts. Java does every bit of the math, in whole cents, so the
 * numbers are exact. The AI only gets to tell the story around them.
 *
 * The rules for each fact are in contract/CONTRACT.md under "Money Wrapped facts".
 * The answer key for the mock data is data/answer_key/wrapped_key.json.
 */
public final class Wrapped {
    private Wrapped() {}

    // ----------------------------------------------------------------- helpers (done for you)

    /** Whole percent, rounded half up, with integer math only. pct(1, 3) is 33, pct(1, 2) is 50. */
    public static long pct(long num, long den) {
        return den == 0 ? 0 : Math.floorDiv(200 * num + den, 2 * den);
    }

    /** "2027-03" becomes "March 2027". */
    public static String monthName(String yearMonth) {
        return YearMonth.parse(yearMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US));
    }

    /** "2027-03-05" becomes "March 5, 2027". */
    public static String dayName(String isoDate) {
        return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US));
    }

    /** Spending rows: not a transfer and not income. Refunds count, and lower spending. */
    public static boolean isSpending(Txn t) {
        return !t.isTransfer && !"Income".equals(t.category);
    }

    public static boolean isWeekend(String isoDate) {
        DayOfWeek d = LocalDate.parse(isoDate).getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    static Fact fact(String id, String label, String text, List<String> values, Map<String, Object> data) {
        return new Fact(id, label, text, values, data);
    }

    static Map<String, Object> data(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        return m;
    }

    /** Running totals for one merchant. */
    static final class Tally {
        long cents;
        int visits;
    }

    // ----------------------------------------------------------------- the facts

    public static List<Fact> facts(List<Txn> rows) {
        List<Fact> out = new ArrayList<>();
        List<Txn> spending = rows.stream().filter(Wrapped::isSpending).toList();
        List<Txn> everyday = spending.stream().filter(t -> !"Housing".equals(t.category)).toList();

        // period (done for you, as an example)
        TreeSet<String> months = new TreeSet<>();
        for (Txn t : rows) months.add(t.month());
        if (months.isEmpty()) return out;
        out.add(fact("period", "The period",
                months.size() + " months of money, " + monthName(months.first()) + " to " + monthName(months.last()),
                List.of(String.valueOf(months.size()), monthName(months.first()), monthName(months.last())),
                data("first_month", months.first(), "last_month", months.last(), "months", months.size())));

        // money_in and money_out
        long moneyIn = rows.stream().filter(t -> !t.isTransfer && "Income".equals(t.category)).mapToLong(t -> t.amountCents).sum();
        long moneyOut = -spending.stream().mapToLong(t -> t.amountCents).sum();
        out.add(fact("money_in", "Money in", "Money in: " + Money.format(moneyIn),
                List.of(Money.format(moneyIn)), data("cents", moneyIn)));
        out.add(fact("money_out", "Money out", "Money out, not counting transfers: " + Money.format(moneyOut),
                List.of(Money.format(moneyOut)), data("cents", moneyOut)));

        // TODO week 3: add the other nine facts, one at a time, in this order. Each one has its rule
        // in contract/CONTRACT.md under "Money Wrapped facts", and its expected answer for the mock
        // data in data/answer_key/wrapped_key.json. Copy the shape of money_in above.
        //
        //   savings_rate        (money in minus money out) as a whole percent of money in: use pct()
        //   top_category        everyday spending (no Housing) by category, biggest first
        //   top_merchant        everyday spending by Money.merchant(description), with visits
        //   most_visited        the merchant with the most money out visits
        //   biggest_month       all spending by month, rent included
        //   weekend_share       spending on Saturdays and Sundays: isWeekend()
        //   biggest_purchase    one money out row, not Housing and not Utilities
        //   recurring           use recurring(spending) below
        //   quiet_subscription  the smallest repeat charge, and what it costs over 12 months
        //
        // Run ./mvnw test -Dweek=3 after each one. The test tells you which fact is next.

        return out;
    }

    /**
     * Merchants that charge about the same amount once a month: money out rows in at least
     * 3 different months, one charge per month, and every charge within 10% of the median
     * (the middle charge; for an even count, the lower middle). Biggest first.
     */
    public static List<Map<String, Object>> recurring(List<Txn> spending) {
        // TODO week 3: find the repeat charges
        // Hint: group money out rows by Money.merchant(description). Keep a merchant when it has rows in
        // at least 3 different months, exactly one charge per month, and every charge within 10% of the median.
        // Hint: return maps made with data("merchant", ..., "monthly_cents", median, "months", n), biggest first.
        throw new UnsupportedOperationException("TODO week 3: find the repeat charges");
    }

    // ----------------------------------------------------------------- cards without AI (done for you)

    /** One plain card per fact. Always true, a little dull: that is where the AI comes in. */
    public static List<Card> templateCards(List<Fact> facts) {
        List<Card> cards = new ArrayList<>();
        for (Fact f : facts) cards.add(new Card("template", f.label(), f.text() + ".", f.id()));
        return cards;
    }
}
