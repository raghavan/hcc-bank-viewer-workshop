package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Week 2: transfers, rules, the review queue, and reading YOUR bank through a profile.
 */
@Week(2)
class Week2Test {
    static Map<String, JsonNode> key;

    @BeforeAll
    static void loadKey() throws IOException {
        key = Fixtures.key(Fixtures.KEY);
    }

    @Test
    void transfersArePairedLikeTheAnswerKey() throws IOException {
        List<Txn> rows = new ArrayList<>(Readers.readMock(false));
        int pairs = Categorizer.markTransfers(rows);
        List<String> wrong = new ArrayList<>();
        for (Txn t : rows) {
            boolean expected = key.get(t.id).path("is_transfer").asBoolean();
            if (t.isTransfer != expected) wrong.add(t.id + " " + t.description + ": is_transfer " + t.isTransfer + ", expected " + expected);
        }
        assertTrue(wrong.isEmpty(), Fixtures.report("Transfers (a pair is opposite amounts, two accounts, within 3 days)", wrong));
        assertEquals(24, pairs, "the mock data has 24 transfer pairs");
        for (Txn t : rows) {
            if (t.isTransfer) {
                assertEquals("Transfer", t.category, t.id + " is a transfer, so its category is Transfer");
                assertTrue(!t.needsReview, t.id + " is a transfer, so it does not need review");
            }
        }
    }

    @Test
    void loadRulesReadsEveryColumn() throws IOException {
        List<Categorizer.Rule> rules = Categorizer.loadRules(Path.of("contract/starter_rules.csv"));
        assertEquals(7, rules.size(), "contract/starter_rules.csv has 7 rules");
        assertEquals(new Categorizer.Rule("I1", "PAYROLL", "Income", 1.0), rules.get(0));
    }

    @Test
    void theFirstMatchingRuleWins() {
        Txn coffee = new Txn("test", 1, "Test", "2027-03-01", "SUNFRESH MARKET #214", -500);
        Txn stall = new Txn("test", 2, "Test", "2027-03-01", "MARKET STALL 7", -500);
        Txn mystery = new Txn("test", 3, "Test", "2027-03-01", "SOMETHING NEW", -500);
        List<Categorizer.Rule> rules = List.of(
                new Categorizer.Rule("G1", "SUNFRESH MARKET", "Groceries", 1.0),
                new Categorizer.Rule("G9", "MARKET", "Groceries", 0.6));
        int placed = Categorizer.applyRules(List.of(coffee, stall, mystery), rules);
        assertEquals(2, placed, "two of the three rows match a rule");
        assertEquals("G1", coffee.ruleId, "G1 comes first, so it wins");
        assertTrue(!coffee.needsReview, "confidence 1.0 means no review needed");
        assertEquals("G9", stall.ruleId);
        assertTrue(stall.needsReview, "confidence 0.6 is below 0.8, so a person should look");
        assertEquals(null, mystery.category, "no rule matched, so applyRules leaves it alone");
    }

    @Test
    void yourRulesPlaceAlmostEveryRowCorrectly() throws IOException {
        List<Txn> rows = Structure.run(Structure.DataSource.MOCK, Categorizer.DEFAULT_RULES, null, false);
        List<String> broken = Invariants.check(rows);
        assertTrue(broken.isEmpty(), Fixtures.report("The contract and the review rule", broken));

        List<String> wrong = new ArrayList<>();
        long decided = 0;
        for (Txn t : rows) {
            if (t.needsReview) continue;
            decided++;
            String expected = key.get(t.id).path("category").asText();
            if (!expected.equals(t.category)) wrong.add(t.id + " " + t.description + ": " + t.category + " (rule " + t.ruleId + "), expected " + expected);
        }
        long right = decided - wrong.size();
        assertTrue(decided > 0 && right * 100 >= decided * 95,
                Fixtures.report("Accuracy on decided rows is " + (decided == 0 ? 0 : right * 100 / decided)
                        + "%, need 95%. Is a pattern too broad? Is RENT catching something that is not rent?", wrong));

        long queue = rows.stream().filter(t -> t.needsReview).count();
        assertTrue(queue >= 1 && queue * 10 <= rows.size(),
                "The review queue has " + queue + " of " + rows.size() + " rows. Aim for at least 1 and at most 10%: "
                        + "add rules for merchants that repeat, and keep a broad low confidence rule for a person to check.");
    }

    @Test
    void aProfileReadsABankYouHaveNeverSeen() throws IOException {
        List<Txn> rows = Readers.fromProfile(Path.of("data/private-example/seagrape.properties"));
        assertEquals(12, rows.size(), "the Seagrape export has 12 transactions (skip the junk line and the # line)");
        Txn first = rows.get(0);
        assertEquals("seagrape-1", first.id);
        assertEquals("2027-04-01", first.date, "date_format MM-dd-yyyy");
        assertEquals("Seagrape Everyday Checking", first.account);
        assertEquals("PAYROLL ACME TECH", first.description);
        assertEquals(271000, first.amountCents, "a deposit is money in");
        assertEquals(-165000, rows.get(1).amountCents, "a withdrawal is money out");
        assertEquals("CORNER BEAN CAFE", rows.get(6).description, "descriptions are cleaned too");
        assertEquals(349729, rows.stream().mapToLong(t -> t.amountCents).sum(), "the month nets to $3,497.29");
    }

    @Test
    void aSignedAmountProfileCanBeFlippedForCards() throws IOException {
        Path dir = Path.of("target", "profile-test");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve("card.csv"), "Date,What,Amount\n2027-04-02,taco tide,23.40\n2027-04-05,PAYMENT THANK YOU,-100.00\n");
        java.nio.file.Files.writeString(dir.resolve("card.properties"), String.join("\n",
                "bank_id=mycard", "account_name=My Card", "file=card.csv", "date_column=Date", "date_format=yyyy-MM-dd",
                "description_column=What", "amount_column=Amount", "sign=flipped", ""));
        List<Txn> rows = Readers.fromProfile(dir.resolve("card.properties"));
        assertEquals(-2340, rows.get(0).amountCents, "on a card a positive purchase is money out");
        assertEquals(10000, rows.get(1).amountCents, "and a payment to the card is money in");
    }
}
