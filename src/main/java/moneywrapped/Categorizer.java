package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Week 2: decide a category for every row.
 *
 * 1. markTransfers: money that only moved between your own accounts is not spending.
 * 2. loadRules and applyRules: the first rule whose pattern is in the description wins.
 * 3. Rows nobody could place become "Other" and wait in the review queue.
 */
public final class Categorizer {
    private Categorizer() {}

    /** The rules file this build uses. Yours starts as a copy of contract/starter_rules.csv. */
    public static final Path DEFAULT_RULES = Path.of("rules.csv");

    public static final List<String> TRANSFER_WORDS = List.of("TRANSFER", "PAYMENT", "AUTOPAY");

    public record Rule(String id, String pattern, String category, double confidence) {}

    /**
     * Find pairs: a description with TRANSFER, PAYMENT or AUTOPAY, two different accounts,
     * opposite amounts (-40000 and +40000), and dates no more than 3 days apart. Mark both
     * rows. Each row joins one pair at most. Returns how many pairs were found.
     */
    public static int markTransfers(List<Txn> rows) {
        // TODO week 2: find the transfer pairs
        // Hint: sort the candidates by date, then id, so you always get the same pairs.
        // Hint: set category Transfer, categorySource "transfer", ruleId "TRANSFER_PAIR", confidence 1.0,
        // needsReview false and isTransfer true on BOTH rows of a pair.
        throw new UnsupportedOperationException("TODO week 2: find the transfer pairs");
    }

    /** Read a rules CSV: rule_id,pattern,category,confidence. */
    public static List<Rule> loadRules(Path file) throws IOException {
        // TODO week 2: read the rules file
        // Hint: Csv.read(file, 0). Clean each pattern with Money.clean so it matches cleaned descriptions.
        // Hint: stop with a clear IllegalArgumentException if a category is not in Categories.ALL.
        throw new UnsupportedOperationException("TODO week 2: read the rules file");
    }

    /**
     * Give each row that is not a transfer the category of the FIRST rule whose pattern
     * appears in its description. Returns how many rows a rule placed.
     */
    public static int applyRules(List<Txn> rows, List<Rule> rules) {
        // TODO week 2: apply the rules
        // Hint: skip transfers. The FIRST rule whose pattern is inside the description wins.
        // Hint: needsReview is true when the rule's confidence is below Categories.REVIEW_THRESHOLD.
        throw new UnsupportedOperationException("TODO week 2: apply the rules");
    }

    // ----------------------------------------------------------------- done for you

    /** Rows nobody could place stay visible as "Other", waiting for a person. */
    public static void finish(List<Txn> rows) {
        for (Txn t : rows) {
            if (t.category == null) {
                t.category = "Other";
                t.categorySource = "none";
                t.needsReview = true;
            }
        }
    }

    /**
     * Apply a decisions.json that the viewer's review queue saved: overrides for single
     * rows, and new rules that run before your rules file. Format in contract/CONTRACT.md.
     */
    public static List<Rule> decisionRules(Path decisions) throws IOException {
        List<Rule> rules = new ArrayList<>();
        if (decisions == null || !Files.exists(decisions)) return rules;
        int i = 0;
        for (JsonNode r : Json.MAPPER.readTree(decisions.toFile()).path("new_rules")) {
            String category = r.path("category").asText();
            if (Categories.ALL.contains(category)) {
                rules.add(new Rule("D" + (++i), Money.clean(r.path("pattern").asText()), category, 1.0));
            }
        }
        return rules;
    }

    public static int applyOverrides(List<Txn> rows, Path decisions) throws IOException {
        if (decisions == null || !Files.exists(decisions)) return 0;
        JsonNode overrides = Json.MAPPER.readTree(decisions.toFile()).path("overrides");
        int changed = 0;
        for (Txn t : rows) {
            JsonNode o = overrides.get(t.id);
            if (o == null || t.isTransfer) continue;
            String category = o.path("category").asText();
            if (!Categories.ALL.contains(category)) continue;
            t.category = category;
            t.categorySource = o.path("source").asText("manual").toLowerCase(Locale.ROOT);
            t.ruleId = null;
            t.confidence = 1.0;
            t.needsReview = false;
            changed++;
        }
        return changed;
    }
}
