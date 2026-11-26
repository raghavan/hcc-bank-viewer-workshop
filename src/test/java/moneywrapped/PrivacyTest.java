package moneywrapped;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Your real bank data must never be committed. This check runs every week. */
@Week(1)
class PrivacyTest {

    @Test
    void gitignoreKeepsPrivateDataAndOutputLocal() throws IOException {
        List<String> lines = Files.readAllLines(Path.of(".gitignore"), StandardCharsets.UTF_8);
        for (String must : List.of("data/private/*", "out/", "target/", "_site/")) {
            assertTrue(lines.contains(must), ".gitignore must contain the line " + must);
        }
    }

    @Test
    void nothingPrivateIsTrackedByGit() throws Exception {
        Process p;
        try {
            p = new ProcessBuilder("git", "ls-files", "--", "data/private", "out", "target", "_site")
                    .redirectErrorStream(true).start();
        } catch (IOException noGit) {
            Assumptions.abort("git is not installed, so this check is skipped");
            return;
        }
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        Assumptions.assumeTrue(p.waitFor() == 0, "not a git repository, so this check is skipped");
        List<String> tracked = output.isEmpty() ? List.of() : List.of(output.split("\\R"));
        assertEquals(List.of("data/private/README.md"), tracked,
                "Only data/private/README.md may be in git. Remove the others with: git rm --cached <file>");
    }
}
