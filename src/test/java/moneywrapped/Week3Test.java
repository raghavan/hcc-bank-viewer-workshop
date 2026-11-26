package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Week 3: Money Wrapped facts, AI cards, and the grounding check.
 * The facts are checked on the TRUE mock rows, so a rule you got wrong last week does not
 * break this week.
 */
@Week(3)
class Week3Test {

    static Map<String, Fact> byId(List<Fact> facts) {
        Map<String, Fact> m = new LinkedHashMap<>();
        for (Fact f : facts) m.put(f.id(), f);
        return m;
    }

    @Test
    void everyFactMatchesTheAnswerKey() throws IOException {
        List<Fact> facts = Wrapped.facts(Fixtures.truthRows());
        Map<String, Fact> got = byId(facts);
        List<String> problems = new ArrayList<>();
        for (JsonNode expected : Json.MAPPER.readTree(Fixtures.WRAPPED_KEY.toFile()).path("facts")) {
            String id = expected.path("id").asText();
            Fact f = got.get(id);
            if (f == null) {
                problems.add(id + " is missing. Expected: " + expected.path("text").asText());
                continue;
            }
            JsonNode mine = Json.MAPPER.readTree(Json.MAPPER.writeValueAsString(f));
            if (!expected.path("data").equals(mine.path("data"))) {
                problems.add(id + ": data is " + mine.path("data") + ", expected " + expected.path("data"));
            } else if (!expected.equals(mine)) {
                problems.add(id + ": the numbers are right, the words differ. Got \"" + f.text() + "\", expected \""
                        + expected.path("text").asText() + "\" with values " + expected.path("values"));
            }
        }
        assertTrue(problems.isEmpty(), Fixtures.report("Wrapped facts (rules in contract/CONTRACT.md)", problems));
    }

    @Test
    void groundingFindsEveryNumber() {
        assertEquals(List.of(new java.math.BigDecimal("1956.42"), new java.math.BigDecimal("24"), new java.math.BigDecimal("45")),
                Grounding.numbersIn("$1,956.42 over 24 visits, 45% of it"));
    }

    @Test
    void groundingKeepsTrueCardsAndRejectsMadeUpNumbers() throws IOException {
        Map<String, Fact> facts = byId(Wrapped.facts(Fixtures.truthRows()));
        Card honest = new Card("test", "Number one", "SUNFRESH MARKET got $1,956.42 over 24 visits.", "top_merchant");
        Card rounded = new Card("test", "Rent", "Rent was $1,650 a month.", "recurring");
        Card invented = new Card("test", "Tacos", "You spent $9,999.00 on tacos.", "top_merchant");
        Card titleLie = new Card("test", "Top 3 places", "SUNFRESH MARKET got $1,956.42.", "top_merchant");
        Card wrongFact = new Card("test", "Weekend", "20% on weekends.", "top_merchant");
        Card noSuchFact = new Card("test", "Hmm", "Something nice.", "favorite_color");
        Card citesNothing = new Card("test", "Hmm", "Something nice.");

        assertNull(Grounding.check(honest, facts), "copies numbers exactly from top_merchant, so it is grounded");
        assertNull(Grounding.check(rounded, facts), "$1,650 and $1,650.00 are the same number");
        assertNotNull(Grounding.check(invented, facts), "$9,999.00 is in no fact");
        assertNotNull(Grounding.check(titleLie, facts), "the title has a 3 that no fact has");
        assertNotNull(Grounding.check(wrongFact, facts), "20% is a real number, but not in the fact it cites");
        assertNotNull(Grounding.check(noSuchFact, facts), "there is no fact called favorite_color");
        assertNotNull(Grounding.check(citesNothing, facts), "a card must cite at least one fact");
    }

    @Test
    void templateCardsAreAllGrounded() throws IOException {
        List<Fact> facts = Wrapped.facts(Fixtures.truthRows());
        Map<String, List<Card>> checked = WrappedRun.check(Wrapped.templateCards(facts), facts);
        assertTrue(checked.get("rejected").isEmpty(), "plain cards copy the fact sentence, so all should pass: " + checked.get("rejected"));
    }

    @Test
    void theFakeAiLiesOnceAndGetsCaught() throws IOException {
        List<Fact> facts = Wrapped.facts(Fixtures.truthRows());
        Map<String, List<Card>> checked = WrappedRun.check(AiCards.mock(facts), facts);
        assertEquals(1, checked.get("rejected").size(), "exactly one fake AI card makes up a number");
        assertTrue(checked.get("rejected").get(0).caption.contains("$9,999.00"));
        assertFalse(checked.get("cards").isEmpty());
    }

    @Test
    void theAiSeesFactsButNeverRowsOrAccounts() throws IOException {
        List<Txn> rows = Fixtures.truthRows();
        String sent = Json.MAPPER.writeValueAsString(AiCards.preview(Wrapped.facts(rows)));
        for (Txn t : rows) {
            assertFalse(sent.contains(t.account), "the AI request must not contain the account name " + t.account);
        }
        assertFalse(sent.matches("(?s).*\\b(gulfview|coquina|harborline|palmetto|sunpine)-\\d.*"), "no row ids in the AI request");
        assertFalse(sent.contains("amount_cents"), "no raw rows in the AI request");
    }

    @Test
    void claudeModeWithoutSendOnlyWritesThePreview(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("transactions.json");
        Structure.write(Fixtures.truthRows(), Structure.DataSource.MOCK, in);
        Map<String, Object> doc = WrappedRun.run(in, "claude", false);
        assertEquals("claude (preview only)", doc.get("ai"));
        assertTrue(AiCards.PREVIEW.toFile().exists(), "out/ai_request_preview.json shows what would be sent");
    }
}
