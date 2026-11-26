package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Week 1: read the five mock banks into one clean shape.
 * Start with Money.clean and Money.toCents, then Readers.gulfview, then the other four.
 */
@Week(1)
class Week1Test {
    static Map<String, JsonNode> key;

    @BeforeAll
    static void loadKey() throws IOException {
        key = Fixtures.key(Fixtures.KEY);
    }

    static Path raw(String file) {
        return Path.of("data/raw", file);
    }

    @Test
    void cleanUppercasesTrimsAndSqueezesSpaces() {
        assertEquals("PAYROLL ACME TECH", Money.clean("  payroll   acme tech "));
        assertEquals("GULF COAST GROCERS", Money.clean("gulf  coast grocers  "));
        assertEquals("TACO TIDE", Money.clean("TACO TIDE"));
    }

    @Test
    void toCentsReadsMoneyTextExactly() {
        assertEquals(165000, Money.toCents("1,650.00"), "1,650.00 has a thousands comma");
        assertEquals(30000, Money.toCents("$300.00"), "$300.00 has a dollar sign");
        assertEquals(-2500, Money.toCents("-$25.00"), "-$25.00 is negative");
        assertEquals(10, Money.toCents("0.10"), "0.10 is ten cents. Did a double turn it into 9?");
        assertEquals(1050, Money.toCents("10.5"), "10.5 is $10.50");
        assertEquals(0, Money.toCents(""), "an empty cell is 0");
    }

    @Test
    void gulfviewMatchesTheAnswerKey() throws IOException {
        List<Txn> rows = Readers.gulfview(raw("gulfview_checking.csv"), "Gulfview CU Checking");
        List<String> problems = Fixtures.compareBank("gulfview", rows, key);
        assertTrue(problems.isEmpty(), Fixtures.report("Gulfview", problems));
    }

    @Test
    void coquinaMatchesTheAnswerKey() throws IOException {
        List<Txn> rows = Readers.coquina(raw("coquina_savings.csv"), "Coquina Savings");
        List<String> problems = Fixtures.compareBank("coquina", rows, key);
        assertTrue(problems.isEmpty(), Fixtures.report("Coquina", problems));
    }

    @Test
    void harborlineMatchesTheAnswerKey() throws IOException {
        List<Txn> rows = Readers.harborline(raw("harborline_visa.csv"), "Harborline Visa");
        List<String> problems = Fixtures.compareBank("harborline", rows, key);
        assertTrue(problems.isEmpty(), Fixtures.report("Harborline", problems));
    }

    @Test
    void palmettoMatchesTheAnswerKey() throws IOException {
        List<Txn> rows = Readers.palmetto(raw("palmetto_mastercard.csv"), "Palmetto Mastercard");
        List<String> problems = Fixtures.compareBank("palmetto", rows, key);
        assertTrue(problems.isEmpty(), Fixtures.report("Palmetto", problems));
    }

    @Test
    void sunpineMatchesTheAnswerKey() throws IOException {
        List<Txn> rows = Readers.sunpine(raw("sunpine_debit.csv"), "Sunpine Debit");
        List<String> problems = Fixtures.compareBank("sunpine", rows, key);
        assertTrue(problems.isEmpty(), Fixtures.report("Sunpine", problems));
    }

    @Test
    void allFiveBanksTogetherGiveEveryRowOnce() throws IOException {
        List<Txn> rows = Readers.readMock(false);
        assertEquals(key.size(), rows.size(), "the five files hold " + key.size() + " rows in total");
        Set<String> ids = new HashSet<>();
        for (Txn t : rows) assertTrue(ids.add(t.id), "id " + t.id + " appears twice");
    }

    @Test
    void structureWritesTheContractFile(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("transactions.json");
        Structure.write(Readers.readMock(false), Structure.DataSource.MOCK, out);
        JsonNode doc = Json.MAPPER.readTree(out.toFile());
        assertEquals("1.0", doc.path("schema_version").asText());
        assertEquals("mock", doc.path("data_source").asText());
        JsonNode first = doc.path("transactions").get(0);
        for (String field : List.of("id", "date", "account", "description", "amount_cents", "category",
                "category_source", "rule_id", "confidence", "needs_review", "is_transfer")) {
            assertTrue(first.has(field), "every row needs the field " + field);
        }
        assertTrue(first.path("amount_cents").isIntegralNumber(), "amount_cents must be a whole number of cents");
    }
}
