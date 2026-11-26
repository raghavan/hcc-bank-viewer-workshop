package moneywrapped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Week 4 (CD): build the folder that gets published, _site/. Done for you.
 *
 * It copies the viewer and your two JSON files, and it REFUSES to publish anything that
 * did not come from the mock data. Your own bank data must never end up on a website.
 */
public final class Site {
    private Site() {}

    public static void build(Path viewerDir, Path outDir, Path siteDir) throws IOException {
        Path tx = outDir.resolve("transactions.json");
        if (!Files.exists(tx)) throw new IOException("No " + tx + " yet. Run ./run structure first.");
        refuseUnlessMock(tx);
        Path wrapped = outDir.resolve("wrapped.json");
        if (Files.exists(wrapped)) refuseUnlessMock(wrapped);

        Files.createDirectories(siteDir);
        try (Stream<Path> files = Files.list(viewerDir)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                Files.copy(f, siteDir.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.copy(tx, siteDir.resolve("transactions.json"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(wrapped)) Files.copy(wrapped, siteDir.resolve("wrapped.json"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Built " + siteDir + " from " + viewerDir + " and " + outDir + " (mock data only)");
    }

    static void refuseUnlessMock(Path file) throws IOException {
        String source = Json.MAPPER.readTree(file.toFile()).path("data_source").asText("unknown");
        if (!"mock".equals(source)) {
            throw new IllegalStateException("Refusing to publish " + file + ": its data_source is \"" + source
                    + "\". Only mock data goes on a website. Run ./run structure (mock data) first.");
        }
    }
}
