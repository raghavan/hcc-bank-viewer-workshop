// My structurer in Java. Same job as the Python starter, same contract, same checker.
//
// Java 17 or newer runs it straight from source, no build tool. From the repo root:
//
//     java my-work/java/Structurer.java
//     python tools/check.py --week 1
//
// The plumbing (CSV splitting, JSON writing) is done for you. The TODOs are yours:
//   Week 1: clean, toCents, the gulfview case in readBank
//   Week 2: the other four cases in readBank
//   Week 3: markTransfers, loadRules, applyRules
//
// Every field rule lives in contract/CONTRACT.md. Output: out/transactions.json

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Structurer {

    static final double REVIEW_THRESHOLD = 0.8;

    /** One clean transaction. The last six fields start "undecided". */
    static final class Txn {
        final String id;
        final LocalDate date;
        final String account;
        final String description;
        final long amountCents;
        String category = null;
        String categorySource = "none";
        String ruleId = null;
        Double confidence = null;
        boolean needsReview = true;
        boolean isTransfer = false;

        Txn(String bank, int rowNumber, String account, LocalDate date, String description, long amountCents) {
            this.id = bank + "-" + rowNumber;
            this.account = account;
            this.date = date;
            this.description = clean(description);
            this.amountCents = amountCents;
        }
    }

    record Rule(String id, String pattern, String category, double confidence) {}

    // ------------------------------------------------------------------ week 1

    /** Uppercase, trim, squeeze runs of spaces to one. */
    static String clean(String text) {
        // TODO (week 1): look at trim(), toUpperCase(Locale.ROOT), split("\\s+") and String.join.
        return text;
    }

    /** "1,650.00" or "$300.00" -> 165000. An empty cell is 0. */
    static long toCents(String text) {
        // TODO (week 1): strip "$" and ",", then use BigDecimal (never double for money).
        // Hint: BigDecimal has movePointRight(2) and longValueExact().
        return 0;
    }

    /** "03/05/2027" with "MM/dd/yyyy" -> LocalDate 2027-03-05. Done for you. */
    static LocalDate date(String text, String pattern) {
        return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(pattern, Locale.US));
    }

    /** Split one CSV line, honoring "quoted, fields". */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cur.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    /** Read a CSV into maps keyed by header, skipping junk, blank and # lines. */
    static List<Map<String, String>> readCsv(Path path, int skipLines) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> data = new ArrayList<>();
        for (String line : lines.subList(skipLines, lines.size())) {
            if (!line.isBlank() && !line.stripLeading().startsWith("#")) data.add(line);
        }
        List<String> header = splitCsv(data.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : data.subList(1, data.size())) {
            List<String> cells = splitCsv(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) row.put(header.get(i), i < cells.size() ? cells.get(i) : "");
            rows.add(row);
        }
        return rows;
    }

    // ------------------------------------------------------------------ weeks 1 and 2

    static List<Txn> readBank(String bank, Path file, String account) throws IOException {
        List<Txn> out = new ArrayList<>();
        int skip = 0; // TODO (week 2): one bank has 3 junk lines before its header
        int n = 0;
        for (Map<String, String> r : readCsv(file, skip)) {
            n++;
            switch (bank) {
                case "gulfview" -> {
                    // TODO (week 1): amount is Credit cents minus Debit cents. Dates are MM/dd/yyyy.
                    // out.add(new Txn(bank, n, account, date(r.get("Date"), "MM/dd/yyyy"), r.get("Description"), amount));
                }
                case "coquina", "harborline", "palmetto", "sunpine" -> {
                    // TODO (week 2): one case per bank. Open the CSV and read the table in CONTRACT.md first.
                }
                default -> throw new IllegalArgumentException("No reader for bank " + bank);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ week 3

    /** Mark pairs of money moving between your own accounts. Return how many pairs. */
    static int markTransfers(List<Txn> all) {
        // TODO (week 3): TRANSFER, PAYMENT or AUTOPAY in the description, different accounts,
        // opposite amounts, at most 3 days apart. Mark both rows (category "Transfer").
        return 0;
    }

    static List<Rule> loadRules(Path path) throws IOException {
        // TODO (week 3): readCsv(path, 0) gives maps with rule_id, pattern, category, confidence.
        return new ArrayList<>();
    }

    /** First matching rule wins. Skip transfers. Return how many rows a rule placed. */
    static int applyRules(List<Txn> all, List<Rule> rules) {
        // TODO (week 3). needsReview is true when the rule's confidence is below REVIEW_THRESHOLD.
        return 0;
    }

    // ------------------------------------------------------------------ JSON out (done for you)

    static String q(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    static String toJson(List<Txn> all) {
        StringBuilder b = new StringBuilder();
        b.append("{\n \"schema_version\": \"1.0\",\n \"generator\": \"my-work/java\",\n \"transactions\": [\n");
        for (int i = 0; i < all.size(); i++) {
            Txn t = all.get(i);
            b.append("  {")
             .append("\"id\": ").append(q(t.id))
             .append(", \"date\": ").append(q(t.date.toString()))
             .append(", \"account\": ").append(q(t.account))
             .append(", \"description\": ").append(q(t.description))
             .append(", \"amount_cents\": ").append(t.amountCents)
             .append(", \"category\": ").append(q(t.category))
             .append(", \"category_source\": ").append(q(t.categorySource))
             .append(", \"rule_id\": ").append(q(t.ruleId))
             .append(", \"confidence\": ").append(t.confidence == null ? "null" : t.confidence.toString())
             .append(", \"needs_review\": ").append(t.needsReview)
             .append(", \"is_transfer\": ").append(t.isTransfer)
             .append(i + 1 < all.size() ? "},\n" : "}\n");
        }
        return b.append(" ]\n}\n").toString();
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws IOException {
        Path rulesFile = Path.of(Files.exists(Path.of("my-work/rules.csv")) ? "my-work/rules.csv" : "contract/starter_rules.csv");
        List<Txn> all = new ArrayList<>();
        for (Map<String, String> acct : readCsv(Path.of("contract/accounts.csv"), 0)) {
            List<Txn> rows = readBank(acct.get("bank_id"), Path.of("data/raw").resolve(acct.get("file")), acct.get("account_name"));
            System.out.printf("  %-11s %4d rows%n", acct.get("bank_id"), rows.size());
            all.addAll(rows);
        }
        System.out.printf("  transfers   %4d pairs%n", markTransfers(all));
        System.out.printf("  rules       %4d rows placed%n", applyRules(all, loadRules(rulesFile)));
        for (Txn t : all) if (t.category == null) t.category = "Other"; // waits for a person

        all.sort(Comparator.comparing((Txn t) -> t.date).thenComparing(t -> t.id));
        Path out = Path.of("out/transactions.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, toJson(all), StandardCharsets.UTF_8);
        System.out.println("Wrote " + all.size() + " rows to " + out);
    }
}
