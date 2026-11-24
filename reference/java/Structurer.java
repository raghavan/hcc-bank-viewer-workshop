// Reference structurer in Java (the instructor's demo build).
//
// One file, no libraries, no build tool. Java 17 or newer can run it straight from source:
//
//     java reference/java/Structurer.java
//
// It does the same job as reference/python/structure.py: read the five messy bank
// exports, turn them into one clean list that follows contract/CONTRACT.md, find
// transfers, apply rules, and write JSON. (No AI step here; see docs/INSTRUCTOR.md.)
//
// Run it from the repo root. Output: out/reference-java/transactions.json

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Structurer {

    static final double REVIEW_THRESHOLD = 0.8;
    static final List<String> TRANSFER_WORDS = List.of("TRANSFER", "PAYMENT", "AUTOPAY");

    /** One clean transaction. Mutable on purpose: categorizing fills in the last fields. */
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

    // ------------------------------------------------------------------ helpers

    static String clean(String text) {
        return String.join(" ", text.trim().toUpperCase(Locale.ROOT).split("\\s+"));
    }

    /** "$1,650.00" -> 165000. BigDecimal keeps the cents exact. */
    static long toCents(String text) {
        String t = text.trim().replace("$", "").replace(",", "");
        if (t.isEmpty()) return 0;
        return new BigDecimal(t).movePointRight(2).longValueExact();
    }

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

    // ------------------------------------------------------------------ one reader per bank

    static List<Txn> readBank(String bank, Path file, String account) throws IOException {
        List<Txn> out = new ArrayList<>();
        int skip = bank.equals("palmetto") ? 3 : 0;
        int n = 0;
        for (Map<String, String> r : readCsv(file, skip)) {
            n++;
            switch (bank) {
                case "gulfview" -> out.add(new Txn(bank, n, account, date(r.get("Date"), "MM/dd/yyyy"),
                        r.get("Description"), toCents(r.get("Credit")) - toCents(r.get("Debit"))));
                case "coquina" -> out.add(new Txn(bank, n, account, date(r.get("Transaction Date"), "MMM d, yyyy"),
                        r.get("Details"), toCents(r.get("Amount (USD)"))));
                case "harborline" -> out.add(new Txn(bank, n, account, date(r.get("Trans. Date"), "yyyy-MM-dd"),
                        r.get("Description"), -toCents(r.get("Amount")))); // card sign is flipped
                case "palmetto" -> {
                    long amount = toCents(r.get("Amount"));
                    if (r.get("Type").trim().equalsIgnoreCase("SALE")) amount = -amount;
                    out.add(new Txn(bank, n, account, date(r.get("Date"), "M/d/yy"), r.get("Merchant"), amount));
                }
                case "sunpine" -> {
                    long amount = toCents(r.get("AMT"));
                    if (r.get("DR_CR").trim().equalsIgnoreCase("DR")) amount = -amount;
                    out.add(new Txn(bank, n, account, LocalDate.parse(r.get("DATE_TIME").trim().substring(0, 10)),
                            r.get("DESC"), amount));
                }
                default -> throw new IllegalArgumentException("No reader for bank " + bank);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ categorize

    static int markTransfers(List<Txn> all) {
        List<Txn> candidates = all.stream()
                .filter(t -> TRANSFER_WORDS.stream().anyMatch(t.description::contains)).toList();
        int pairs = 0;
        for (Txn out : candidates.stream().filter(t -> t.amountCents < 0)
                .sorted(Comparator.comparing((Txn t) -> t.date)).toList()) {
            for (Txn in : candidates.stream().filter(t -> t.amountCents > 0)
                    .sorted(Comparator.comparing((Txn t) -> t.date)).toList()) {
                if (in.isTransfer || in.account.equals(out.account)) continue;
                if (in.amountCents != -out.amountCents) continue;
                if (Math.abs(ChronoUnit.DAYS.between(out.date, in.date)) > 3) continue;
                for (Txn t : List.of(out, in)) {
                    t.category = "Transfer";
                    t.categorySource = "transfer";
                    t.ruleId = "TRANSFER_PAIR";
                    t.confidence = 1.0;
                    t.needsReview = false;
                    t.isTransfer = true;
                }
                pairs++;
                break;
            }
        }
        return pairs;
    }

    static List<Rule> loadRules(Path path) throws IOException {
        List<Rule> rules = new ArrayList<>();
        for (Map<String, String> r : readCsv(path, 0)) {
            rules.add(new Rule(r.get("rule_id"), r.get("pattern").toUpperCase(Locale.ROOT),
                    r.get("category"), Double.parseDouble(r.get("confidence"))));
        }
        return rules;
    }

    static int applyRules(List<Txn> all, List<Rule> rules) {
        int placed = 0;
        for (Txn t : all) {
            if (t.isTransfer) continue;
            for (Rule rule : rules) {
                if (t.description.contains(rule.pattern())) {
                    t.category = rule.category();
                    t.categorySource = "rule";
                    t.ruleId = rule.id();
                    t.confidence = rule.confidence();
                    t.needsReview = rule.confidence() < REVIEW_THRESHOLD;
                    placed++;
                    break;
                }
            }
            if (t.category == null) t.category = "Other"; // nobody placed it: waits for a person
        }
        return placed;
    }

    // ------------------------------------------------------------------ JSON out

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
        b.append("{\n \"schema_version\": \"1.0\",\n \"generator\": \"reference/java\",\n \"transactions\": [\n");
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
        Path raw = Path.of(args.length > 0 ? args[0] : "data/raw");
        Path out = Path.of(args.length > 1 ? args[1] : "out/reference-java/transactions.json");
        Path rulesFile = Path.of(args.length > 2 ? args[2] : "reference/rules.csv");

        List<Txn> all = new ArrayList<>();
        System.out.println("1. Reading bank files");
        for (Map<String, String> acct : readCsv(Path.of("contract/accounts.csv"), 0)) {
            List<Txn> rows = readBank(acct.get("bank_id"), raw.resolve(acct.get("file")), acct.get("account_name"));
            System.out.printf("  %-26s %4d rows%n", acct.get("file"), rows.size());
            all.addAll(rows);
        }
        System.out.println("2. Finding transfers: " + markTransfers(all) + " pairs");
        System.out.println("3. Applying rules: " + applyRules(all, loadRules(rulesFile)) + " rows placed");

        all.sort(Comparator.comparing((Txn t) -> t.date).thenComparing(t -> t.id));
        Files.createDirectories(out.getParent());
        Files.writeString(out, toJson(all), StandardCharsets.UTF_8);
        long review = all.stream().filter(t -> t.needsReview).count();
        System.out.println("\nWrote " + all.size() + " transactions to " + out);
        System.out.println(review + " rows are waiting in the review queue");
    }
}
