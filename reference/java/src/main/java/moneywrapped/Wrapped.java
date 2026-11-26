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

        // savings_rate
        long saved = moneyIn - moneyOut;
        long rate = pct(saved, moneyIn);
        out.add(fact("savings_rate", "Savings rate", "You kept " + rate + "% of what came in: " + Money.format(saved),
                List.of(rate + "%", Money.format(saved)), data("pct", rate, "saved_cents", saved)));

        // top_category: everyday spending, so rent does not win every time
        Map<String, Long> byCategory = new TreeMap<>();
        for (Txn t : everyday) byCategory.merge(t.category, -t.amountCents, Long::sum);
        byCategory.entrySet().stream()
                .min(Comparator.comparing((Map.Entry<String, Long> e) -> -e.getValue()).thenComparing(Map.Entry::getKey))
                .ifPresent(e -> {
                    long share = pct(e.getValue(), moneyOut);
                    out.add(fact("top_category", "Top everyday category",
                            "Your top everyday category: " + e.getKey() + ", " + Money.format(e.getValue())
                                    + " (" + share + "% of money out)",
                            List.of(Money.format(e.getValue()), share + "%"),
                            data("name", e.getKey(), "cents", e.getValue(), "pct_of_out", share)));
                });

        // top_merchant and most_visited
        Map<String, Tally> byMerchant = new TreeMap<>();
        for (Txn t : everyday) {
            Tally tally = byMerchant.computeIfAbsent(Money.merchant(t.description), k -> new Tally());
            tally.cents -= t.amountCents;
            if (t.amountCents < 0) tally.visits++;
        }
        byMerchant.entrySet().stream()
                .min(Comparator.comparing((Map.Entry<String, Tally> e) -> -e.getValue().cents).thenComparing(Map.Entry::getKey))
                .ifPresent(e -> out.add(fact("top_merchant", "Top merchant",
                        "Where most of your everyday money went: " + e.getKey() + ", " + Money.format(e.getValue().cents)
                                + " over " + e.getValue().visits + " visits",
                        List.of(Money.format(e.getValue().cents), String.valueOf(e.getValue().visits)),
                        data("name", e.getKey(), "cents", e.getValue().cents, "visits", e.getValue().visits))));
        byMerchant.entrySet().stream()
                .min(Comparator.comparing((Map.Entry<String, Tally> e) -> -e.getValue().visits)
                        .thenComparing(e -> -e.getValue().cents).thenComparing(Map.Entry::getKey))
                .ifPresent(e -> out.add(fact("most_visited", "Most visited",
                        "Your most visited place: " + e.getKey() + ", " + e.getValue().visits + " visits and "
                                + Money.format(e.getValue().cents),
                        List.of(String.valueOf(e.getValue().visits), Money.format(e.getValue().cents)),
                        data("name", e.getKey(), "visits", e.getValue().visits, "cents", e.getValue().cents))));

        // biggest_month: all spending, rent included
        Map<String, Long> byMonth = new TreeMap<>();
        for (Txn t : spending) byMonth.merge(t.month(), -t.amountCents, Long::sum);
        byMonth.entrySet().stream()
                .min(Comparator.comparing((Map.Entry<String, Long> e) -> -e.getValue()).thenComparing(Map.Entry::getKey))
                .ifPresent(e -> out.add(fact("biggest_month", "Biggest month",
                        "Your biggest spending month: " + monthName(e.getKey()) + ", " + Money.format(e.getValue()),
                        List.of(monthName(e.getKey()), Money.format(e.getValue())),
                        data("month", e.getKey(), "cents", e.getValue()))));

        // weekend_share
        long weekend = -spending.stream().filter(t -> isWeekend(t.date)).mapToLong(t -> t.amountCents).sum();
        long weekendPct = pct(weekend, moneyOut);
        out.add(fact("weekend_share", "Weekend spending",
                weekendPct + "% of your money out went on Saturdays and Sundays: " + Money.format(weekend),
                List.of(weekendPct + "%", Money.format(weekend)), data("pct", weekendPct, "cents", weekend)));

        // biggest_purchase: one money out row, not rent and not a bill
        spending.stream()
                .filter(t -> t.amountCents < 0 && !"Housing".equals(t.category) && !"Utilities".equals(t.category))
                .min(Comparator.comparingLong((Txn t) -> t.amountCents).thenComparing(t -> t.date).thenComparing(t -> t.id))
                .ifPresent(t -> out.add(fact("biggest_purchase", "Biggest single purchase",
                        "Your biggest single purchase: " + t.description + ", " + Money.format(-t.amountCents)
                                + " on " + dayName(t.date),
                        List.of(Money.format(-t.amountCents), dayName(t.date)),
                        data("description", t.description, "date", t.date, "cents", -t.amountCents))));

        // recurring and quiet_subscription
        List<Map<String, Object>> items = recurring(spending);
        if (!items.isEmpty()) {
            long total = items.stream().mapToLong(i -> (long) i.get("monthly_cents")).sum();
            List<String> values = new ArrayList<>(List.of(String.valueOf(items.size()), Money.format(total)));
            List<String> listing = new ArrayList<>();
            for (Map<String, Object> i : items) {
                values.add(Money.format((long) i.get("monthly_cents")));
                listing.add(i.get("merchant") + " " + Money.format((long) i.get("monthly_cents")));
            }
            out.add(fact("recurring", "Repeat charges",
                    items.size() + " charges repeat every month, " + Money.format(total) + " a month in total: "
                            + String.join(", ", listing),
                    values, data("count", items.size(), "total_monthly_cents", total, "items", items)));
            Map<String, Object> q = items.get(items.size() - 1);
            long monthly = (long) q.get("monthly_cents");
            out.add(fact("quiet_subscription", "Quietest repeat charge",
                    "Your smallest repeat charge: " + q.get("merchant") + " at " + Money.format(monthly) + " a month, "
                            + Money.format(12 * monthly) + " a year",
                    List.of(Money.format(monthly), Money.format(12 * monthly)),
                    data("merchant", q.get("merchant"), "monthly_cents", monthly, "yearly_cents", 12 * monthly)));
        }
        return out;
    }

    /**
     * Merchants that charge about the same amount once a month: money out rows in at least
     * 3 different months, one charge per month, and every charge within 10% of the median
     * (the middle charge; for an even count, the lower middle). Biggest first.
     */
    public static List<Map<String, Object>> recurring(List<Txn> spending) {
        Map<String, List<Txn>> groups = new TreeMap<>();
        for (Txn t : spending) {
            if (t.amountCents < 0) groups.computeIfAbsent(Money.merchant(t.description), k -> new ArrayList<>()).add(t);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, List<Txn>> g : groups.entrySet()) {
            List<Long> charges = g.getValue().stream().map(t -> -t.amountCents).sorted().toList();
            TreeSet<String> seen = new TreeSet<>();
            for (Txn t : g.getValue()) seen.add(t.month());
            long median = charges.get((charges.size() - 1) / 2);
            boolean steady = charges.stream().allMatch(c -> Math.abs(c - median) * 10 <= median);
            if (seen.size() >= 3 && charges.size() == seen.size() && steady) {
                items.add(data("merchant", g.getKey(), "monthly_cents", median, "months", seen.size()));
            }
        }
        items.sort(Comparator.comparing((Map<String, Object> i) -> -(long) i.get("monthly_cents"))
                .thenComparing(i -> (String) i.get("merchant")));
        return items;
    }

    // ----------------------------------------------------------------- cards without AI (done for you)

    /** One plain card per fact. Always true, a little dull: that is where the AI comes in. */
    public static List<Card> templateCards(List<Fact> facts) {
        List<Card> cards = new ArrayList<>();
        for (Fact f : facts) cards.add(new Card("template", f.label(), f.text() + ".", f.id()));
        return cards;
    }
}
