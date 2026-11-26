package moneywrapped;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Week 3: AI writes the Wrapped cards. Done for you, so the lesson is the loop around it:
 * Java computes the facts, the AI tells the story, and Grounding checks every number.
 *
 * Three modes:
 *   off     no AI, one plain card per fact
 *   mock    a fake AI with fixed answers, free and offline, and one answer is a lie on purpose
 *   claude  the real Claude API. Sends ONLY the facts below, never your rows, and only with --send
 *
 * Whatever the mode, out/ai_request_preview.json shows exactly what would be sent.
 */
public final class AiCards {
    private AiCards() {}

    public static final Path PREVIEW = Path.of("out/ai_request_preview.json");
    public static final String DEFAULT_MODEL = "claude-opus-5-5";
    public static final long MAX_TOKENS = 16000;

    /** You can tune these instructions. Try a change, run again, and read what comes back. */
    public static final String SYSTEM = """
            You write Money Wrapped cards: a fun, kind year in review of someone's spending, \
            like a music app's yearly recap, but for a bank account.
            You get facts that a program already computed. Rules:
            1. Use only the facts given. Each card lists the ids of the facts it uses in fact_ids.
            2. Copy every number exactly as it is written in that fact's values, with the $ sign and commas. \
            Never add, round, estimate or invent a number, and do not write numbers as words.
            3. Be encouraging, never shaming. Where it fits, end with one small, practical idea.
            4. Look across the facts for something a person might miss, like two services that do the \
            same job or a small habit that adds up, and say it, still without new numbers.
            Write between 4 and 6 cards.""";

    /** What the model must send back. The descriptions become part of the JSON schema it follows. */
    public static class Reply {
        @JsonPropertyDescription("Between 4 and 6 cards")
        public List<Item> cards = new ArrayList<>();
    }

    public static class Item {
        @JsonProperty("fact_ids")
        @JsonPropertyDescription("Ids of the facts this card uses, copied exactly from the facts list")
        public List<String> factIds = new ArrayList<>();

        @JsonPropertyDescription("A short, fun headline, at most 8 words")
        public String title;

        @JsonPropertyDescription("One or two friendly sentences. Every number is copied exactly from the cited facts")
        public String caption;
    }

    /** The facts as the AI sees them: id, label, sentence and values. No accounts, no row ids. */
    public static List<Map<String, Object>> factsForAi(List<Fact> facts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Fact f : facts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.id());
            m.put("label", f.label());
            m.put("text", f.text());
            m.put("values", f.values());
            out.add(m);
        }
        return out;
    }

    public static String userMessage(List<Fact> facts) throws JsonProcessingException {
        return "Here are the facts, as JSON:\n" + Json.MAPPER.writeValueAsString(factsForAi(facts));
    }

    public static String model() {
        String m = System.getenv("MONEY_WRAPPED_MODEL");
        return m == null || m.isBlank() ? DEFAULT_MODEL : m.trim();
    }

    /** Everything a real request would carry, written to out/ai_request_preview.json. */
    public static Map<String, Object> preview(List<Fact> facts) throws JsonProcessingException {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("note", "This is exactly what --ai claude --send would send to the Claude API. Nothing else leaves your computer.");
        p.put("model", model());
        p.put("max_tokens", MAX_TOKENS);
        p.put("system", SYSTEM);
        p.put("messages", List.of(Map.of("role", "user", "content", userMessage(facts))));
        p.put("reply_format", "JSON: {\"cards\": [{\"fact_ids\": [...], \"title\": \"...\", \"caption\": \"...\"}]}");
        return p;
    }

    public static void writePreview(List<Fact> facts) throws IOException {
        Json.write(PREVIEW, preview(facts));
    }

    // ----------------------------------------------------------------- the fake AI

    /**
     * Fixed, friendly answers built from your facts, so they change when your data does.
     * The last card makes up a number on purpose. The grounding check should catch it.
     */
    public static List<Card> mock(List<Fact> facts) {
        Map<String, Fact> by = new LinkedHashMap<>();
        for (Fact f : facts) by.put(f.id(), f);
        List<Card> cards = new ArrayList<>();
        Fact f;
        if ((f = by.get("most_visited")) != null) {
            cards.add(new Card("mock_ai", "Your regular spot",
                    f.data().get("name") + " saw you " + f.values().get(0) + " times. Some people have a gym. You have this.",
                    "most_visited"));
        }
        if ((f = by.get("top_merchant")) != null) {
            cards.add(new Card("mock_ai", "Number one, by a mile",
                    f.data().get("name") + " got " + f.values().get(0) + " of your everyday money.", "top_merchant"));
        }
        if ((f = by.get("quiet_subscription")) != null) {
            cards.add(new Card("mock_ai", "The quiet one",
                    f.data().get("merchant") + " takes " + f.values().get(0) + " a month without a sound. That is "
                            + f.values().get(1) + " a year. Still using it?", "quiet_subscription"));
        }
        if ((f = by.get("savings_rate")) != null) {
            cards.add(new Card("mock_ai", "You paid yourself too",
                    "You kept " + f.values().get(0) + " of everything that came in. Future you says thanks.", "savings_rate"));
        }
        if ((f = by.get("weekend_share")) != null) {
            cards.add(new Card("mock_ai", "Weekend mode",
                    f.values().get(0) + " of your spending happened on weekends.", "weekend_share"));
        }
        cards.add(new Card("mock_ai", "Taco legend",
                "You spent $9,999.00 on tacos this year. Respect.", "top_merchant"));
        return cards;
    }

    // ----------------------------------------------------------------- the real thing

    /**
     * Ask Claude for cards. Needs the ANTHROPIC_API_KEY environment variable, and costs a
     * little money per run, which is why it only runs with --send.
     */
    public static List<Card> claude(List<Fact> facts) throws IOException {
        if (System.getenv("ANTHROPIC_API_KEY") == null) {
            throw new IllegalStateException("Set ANTHROPIC_API_KEY first (see weeks/week-3/README.md). Nothing was sent.");
        }
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        try {
            StructuredMessageCreateParams<Reply> params = MessageCreateParams.builder()
                    .model(model())
                    .maxTokens(MAX_TOKENS)
                    .system(SYSTEM)
                    .addUserMessage(userMessage(facts))
                    .outputConfig(Reply.class)
                    .build();
            StructuredMessage<Reply> reply = client.messages().create(params);
            List<Card> cards = new ArrayList<>();
            reply.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(text -> {
                        for (Item i : text.text().cards) {
                            Card c = new Card("claude", i.title, i.caption);
                            c.factIds = i.factIds == null ? new ArrayList<>() : new ArrayList<>(i.factIds);
                            cards.add(c);
                        }
                    });
            return cards;
        } finally {
            client.close();
        }
    }
}
