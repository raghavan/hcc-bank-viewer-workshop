package moneywrapped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Week 3: the grounding check. An AI writes lovely sentences and sometimes makes up a
 * number. Before a card reaches a person, we check every number in it against the facts
 * Java computed. One number we cannot find, and the card is rejected.
 */
public final class Grounding {
    private Grounding() {}

    /** Money like $1,956.42, percents like 45%, plain counts like 24. */
    static final Pattern NUMBER = Pattern.compile("\\$?\\d[\\d,]*(?:\\.\\d+)?%?");

    /** Every number in a piece of text, as exact decimals: "$1,956.42" is 1956.42, "45%" is 45. */
    public static List<BigDecimal> numbersIn(String text) {
        List<BigDecimal> found = new ArrayList<>();
        if (text == null) return found;
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String digits = m.group().replace("$", "").replace(",", "").replace("%", "");
            found.add(new BigDecimal(digits).stripTrailingZeros());
        }
        return found;
    }

    /**
     * Returns null when the card is grounded, or the reason it is not:
     * it cites no fact, cites a fact that does not exist, or has a number (in the title or
     * the caption) that none of its cited facts contains, in its sentence or its values.
     */
    public static String check(Card card, Map<String, Fact> factsById) {
        // TODO week 3: write the grounding check
        // Hint: return null when the card is fine, or a short reason when it is not.
        // Hint: collect numbersIn(...) of every cited fact's text and values. Every number in the
        // card's title and caption must be in that set. A card that cites no facts fails too.
        throw new UnsupportedOperationException("TODO week 3: write the grounding check");
    }
}
