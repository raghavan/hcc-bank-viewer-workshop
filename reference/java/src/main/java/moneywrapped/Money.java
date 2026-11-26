package moneywrapped;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Small text and money helpers. Week 1. */
public final class Money {
    private Money() {}

    /** "  payroll   acme tech " becomes "PAYROLL ACME TECH": uppercase, trimmed, single spaces. */
    public static String clean(String text) {
        if (text == null) return "";
        return String.join(" ", text.trim().toUpperCase(Locale.ROOT).split("\\s+"));
    }

    /**
     * Money text to whole cents: "1,650.00" is 165000, "$300.00" is 30000, "-$25.00" is
     * -2500, and an empty cell is 0. BigDecimal keeps every cent exact; a double would not.
     */
    public static long toCents(String text) {
        if (text == null) return 0;
        String t = text.trim().replace("$", "").replace(",", "");
        if (t.isEmpty()) return 0;
        return new BigDecimal(t).movePointRight(2).longValueExact();
    }

    /** 165000 becomes "$1,650.00" and -675 becomes "-$6.75". Done for you. */
    public static String format(long cents) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String text = "$" + nf.format(BigDecimal.valueOf(Math.abs(cents), 2));
        return cents < 0 ? "-" + text : text;
    }

    /**
     * The merchant behind a description, so that "SUNFRESH MARKET #214" and
     * "SUNFRESH MARKET #88" count as one place. Drops trailing words that hold a digit or
     * start with #, then a trailing REFUND. Done for you.
     */
    public static String merchant(String description) {
        List<String> words = new ArrayList<>(Arrays.asList(clean(description).split(" ")));
        while (!words.isEmpty()) {
            String last = words.get(words.size() - 1);
            if (last.startsWith("#") || last.chars().anyMatch(Character::isDigit)) words.remove(words.size() - 1);
            else break;
        }
        if (words.size() > 1 && words.get(words.size() - 1).equals("REFUND")) words.remove(words.size() - 1);
        return words.isEmpty() ? clean(description) : String.join(" ", words);
    }
}
