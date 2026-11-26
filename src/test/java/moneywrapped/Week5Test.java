package moneywrapped;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Week 5 (open lab, optional): a sixth bank that exports JSON instead of CSV. */
@Week(5)
class Week5Test {
    @Test
    void tarponMatchesTheBonusKey() throws IOException {
        List<Txn> rows = Readers.tarpon(Path.of("data/bonus/tarpon_checking.json"), "Tarpon Trust Checking");
        List<String> problems = Fixtures.compareBank("tarpon", rows, Fixtures.key(Fixtures.BONUS_KEY));
        assertTrue(problems.isEmpty(), Fixtures.report("Tarpon", problems));
    }
}
