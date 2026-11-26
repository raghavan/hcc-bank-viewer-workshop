package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared test helpers: the answer keys, and friendly failure messages. */
final class Fixtures {
    private Fixtures() {}

    static final Path KEY = Path.of("data/answer_key/answer_key.json");
    static final Path WRAPPED_KEY = Path.of("data/answer_key/wrapped_key.json");
    static final Path BONUS_KEY = Path.of("data/bonus/answer_key_bonus.json");

    /** The answer key rows by id, in file order. */
    static Map<String, JsonNode> key(Path file) throws IOException {
        Map<String, JsonNode> rows = new LinkedHashMap<>();
        for (JsonNode r : Json.MAPPER.readTree(file.toFile()).path("rows")) rows.put(r.path("id").asText(), r);
        return rows;
    }

    /** The true rows (with true categories) as Txn objects, for checks that should not depend on your rules. */
    static List<Txn> truthRows() throws IOException {
        List<Txn> rows = new ArrayList<>();
        for (JsonNode r : key(KEY).values()) {
            Txn t = new Txn();
            t.id = r.path("id").asText();
            t.date = r.path("date").asText();
            t.account = r.path("account").asText();
            t.description = r.path("description").asText();
            t.amountCents = r.path("amount_cents").asLong();
            t.category = r.path("category").asText();
            t.isTransfer = r.path("is_transfer").asBoolean();
            t.categorySource = t.isTransfer ? "transfer" : "rule";
            t.confidence = 1.0;
            t.needsReview = false;
            rows.add(t);
        }
        return rows;
    }

    /** Compare one bank's rows with the answer key. Returns problems, empty when all is well. */
    static List<String> compareBank(String bank, List<Txn> rows, Map<String, JsonNode> key) {
        List<String> problems = new ArrayList<>();
        List<JsonNode> expected = key.values().stream().filter(k -> k.path("id").asText().startsWith(bank + "-")).toList();
        Map<String, Txn> byId = rows.stream().collect(Collectors.toMap(t -> t.id, t -> t, (a, b) -> a, LinkedHashMap::new));
        if (rows.size() != expected.size()) {
            problems.add("expected " + expected.size() + " rows from " + bank + ", got " + rows.size()
                    + ". Count data rows only, starting at 1: skip the header, junk lines, blank lines and # lines.");
        }
        for (JsonNode k : expected) {
            String id = k.path("id").asText();
            Txn t = byId.get(id);
            if (t == null) {
                problems.add(id + " is missing (ids are " + bank + "-1, " + bank + "-2, ... in file order)");
                continue;
            }
            same(problems, id, "date", k.path("date").asText(), t.date, "check the date pattern in the CONTRACT.md table");
            same(problems, id, "account", k.path("account").asText(), t.account, "use the account_name from contract/accounts.csv");
            same(problems, id, "description", k.path("description").asText(), t.description, "uppercase, trim, squeeze spaces");
            same(problems, id, "amount_cents", k.path("amount_cents").asLong(), t.amountCents,
                    "negative means money left the account; credit cards flip the sign");
        }
        return problems;
    }

    static void same(List<String> problems, String id, String field, Object expected, Object got, String hint) {
        if (!expected.equals(got)) problems.add(id + ": " + field + " is " + got + ", expected " + expected + "  (hint: " + hint + ")");
    }

    /** A failure message that shows the first few problems, not hundreds. */
    static String report(String what, List<String> problems) {
        StringBuilder b = new StringBuilder(what + ": " + problems.size() + " problem(s)");
        problems.stream().limit(6).forEach(p -> b.append("\n    ").append(p));
        if (problems.size() > 6) b.append("\n    ... and ").append(problems.size() - 6).append(" more");
        return b.toString();
    }
}
