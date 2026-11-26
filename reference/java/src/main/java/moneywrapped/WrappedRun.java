package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The wrapped step from start to finish: read out/transactions.json, compute the facts,
 * get cards, check every card, and write out/wrapped.json. Done for you.
 */
public final class WrappedRun {
    private WrappedRun() {}

    public static final Path OUT = Path.of("out/wrapped.json");

    /** Read any contract file back into rows, whichever language wrote it. */
    public static List<Txn> readTransactions(Path file) throws IOException {
        JsonNode root = Json.MAPPER.readTree(file.toFile());
        ObjectMapper lenient = Json.MAPPER.copy()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Txn> rows = new ArrayList<>();
        for (JsonNode n : root.path("transactions")) rows.add(lenient.treeToValue(n, Txn.class));
        return rows;
    }

    public static String dataSource(Path file) throws IOException {
        return Json.MAPPER.readTree(file.toFile()).path("data_source").asText("unknown");
    }

    /** Split cards into the ones that passed the grounding check and the ones that did not. */
    public static Map<String, List<Card>> check(List<Card> cards, List<Fact> facts) {
        Map<String, Fact> byId = new LinkedHashMap<>();
        for (Fact f : facts) byId.put(f.id(), f);
        List<Card> kept = new ArrayList<>();
        List<Card> rejected = new ArrayList<>();
        for (Card c : cards) {
            String reason = Grounding.check(c, byId);
            if (reason == null) kept.add(c);
            else {
                c.reason = reason;
                rejected.add(c);
            }
        }
        Map<String, List<Card>> out = new LinkedHashMap<>();
        out.put("cards", kept);
        out.put("rejected", rejected);
        return out;
    }

    public static Map<String, Object> run(Path in, String ai, boolean send) throws IOException {
        List<Txn> rows = readTransactions(in);
        String source = dataSource(in);
        List<Fact> facts = Wrapped.facts(rows);
        System.out.println("1. Read " + rows.size() + " rows from " + in + " (" + source + " data)");
        System.out.println("2. Computed " + facts.size() + " facts");
        AiCards.writePreview(facts);

        List<Card> cards;
        String note = null;
        switch (ai) {
            case "off" -> cards = Wrapped.templateCards(facts);
            case "mock" -> cards = AiCards.mock(facts);
            case "claude" -> {
                if (send) {
                    if ("private".equals(source)) {
                        System.out.println("   Sending facts about YOUR money (no rows, no account names) to the Claude API.");
                    }
                    cards = AiCards.claude(facts);
                } else {
                    cards = Wrapped.templateCards(facts);
                    note = "preview only: nothing was sent. Read " + AiCards.PREVIEW + ", then add --send";
                }
            }
            default -> throw new IllegalArgumentException("--ai must be off, mock or claude");
        }
        System.out.println("3. Cards: " + cards.size() + " (" + (note == null ? "ai " + ai : note) + ")");
        Map<String, List<Card>> checked = check(cards, facts);
        System.out.println("4. Grounding check: " + checked.get("cards").size() + " kept, "
                + checked.get("rejected").size() + " rejected");
        for (Card c : checked.get("rejected")) System.out.println("   rejected \"" + c.title + "\": " + c.reason);

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schema_version", "1.0");
        doc.put("generated_at", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        doc.put("data_source", source);
        doc.put("ai", ai + (note == null ? "" : " (preview only)"));
        doc.put("facts", facts);
        doc.put("cards", checked.get("cards"));
        doc.put("rejected", checked.get("rejected"));
        return doc;
    }
}
