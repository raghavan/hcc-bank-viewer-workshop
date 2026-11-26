package moneywrapped;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self checks on YOUR data, where there is no answer key:
 *
 *     ./mvnw test -Dweek=2 -Ddata=private
 *
 * Messages name row ids and counts, never your descriptions, and test reports stay in
 * target/, which git ignores.
 */
@Week(2)
class InvariantTest {

    @Test
    void yourOwnDataFollowsEveryRule() throws IOException {
        Assumptions.assumeTrue("private".equals(System.getProperty("data")), "run with -Ddata=private to check your own data");
        List<Path> profiles = Readers.profiles(Structure.PRIVATE_DIR);
        Assumptions.assumeFalse(profiles.isEmpty(), "no profile in data/private yet (see data/private/README.md)");

        List<Txn> rows = Structure.run(Structure.DataSource.PRIVATE, Categorizer.DEFAULT_RULES, null, false);
        List<String> broken = Invariants.check(rows);
        assertTrue(broken.isEmpty(), Fixtures.report("Your data breaks a rule", broken));

        for (Path profile : profiles) {
            Properties p = new Properties();
            try (Reader in = Files.newBufferedReader(profile, StandardCharsets.UTF_8)) {
                p.load(in);
            }
            String bank = p.getProperty("bank_id").trim();
            Path csv = profile.resolveSibling(p.getProperty("file").trim());
            int skip = Integer.parseInt(p.getProperty("skip_lines", "0").trim());
            int dataLines = Csv.dataLines(csv, skip).size() - 1; // minus the header
            List<String> ids = new ArrayList<>();
            for (Txn t : rows) if (t.id.startsWith(bank + "-")) ids.add(t.id);
            assertEquals(dataLines, ids.size(), profile.getFileName() + ": the file has " + dataLines
                    + " data lines but " + ids.size() + " rows came out. Check skip_lines.");
            for (int i = 1; i <= dataLines; i++) {
                assertTrue(ids.contains(bank + "-" + i), profile.getFileName() + ": row " + bank + "-" + i + " is missing");
            }
        }
        long queue = rows.stream().filter(t -> t.needsReview).count();
        System.out.println("Your data: " + rows.size() + " rows, " + queue + " in the review queue. Add rules until that feels small.");
    }
}
