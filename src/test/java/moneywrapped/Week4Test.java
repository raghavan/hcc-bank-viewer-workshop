package moneywrapped;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Week 4: serve the viewer from Java, and publish only what is safe to publish. */
@Week(4)
class Week4Test {
    static final Path VIEWER = Path.of("viewer");

    static Path outWith(Path dir, String dataSource) throws IOException {
        Files.writeString(dir.resolve("transactions.json"),
                "{\"schema_version\": \"1.0\", \"data_source\": \"" + dataSource + "\", \"transactions\": []}");
        Files.writeString(dir.resolve("wrapped.json"),
                "{\"schema_version\": \"1.0\", \"data_source\": \"" + dataSource + "\", \"facts\": [], \"cards\": []}");
        return dir;
    }

    static HttpResponse<String> get(HttpServer server, String path) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theViewerHasItsFourScreens() throws IOException {
        String html = Files.readString(VIEWER.resolve("index.html"));
        for (String id : new String[] {"wrapped", "summary", "transactions", "review-queue"}) {
            assertTrue(html.contains("id=\"" + id + "\""), "viewer/index.html needs an element with id=\"" + id + "\"");
        }
    }

    @Test
    void theServerAnswersEveryRoute(@TempDir Path tmp) throws Exception {
        Path out = outWith(tmp, "mock");
        HttpServer server = Server.start(0, VIEWER, out);
        try {
            assertTrue(server.getAddress().getAddress().isLoopbackAddress(), "listen on 127.0.0.1 only, so only you can see it");
            HttpResponse<String> page = get(server, "/");
            assertEquals(200, page.statusCode(), "GET / serves viewer/index.html");
            assertTrue(page.body().contains("id=\"wrapped\""));
            assertTrue(page.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
            HttpResponse<String> js = get(server, "/app.js");
            assertEquals(200, js.statusCode(), "GET /app.js serves viewer/app.js");
            assertTrue(js.headers().firstValue("Content-Type").orElse("").contains("javascript"));
            HttpResponse<String> tx = get(server, "/transactions.json");
            assertEquals(200, tx.statusCode(), "GET /transactions.json serves out/transactions.json");
            assertEquals(Files.readString(out.resolve("transactions.json")), tx.body());
            assertEquals(200, get(server, "/wrapped.json").statusCode(), "GET /wrapped.json serves out/wrapped.json");
            assertEquals(404, get(server, "/nope.html").statusCode(), "a missing file is a 404");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void theServerNeverServesFilesOutsideTheViewer(@TempDir Path tmp) {
        assertNull(Server.resolve("/../pom.xml", VIEWER, tmp), "../ must not escape the viewer folder");
        assertNull(Server.resolve("/../data/private/README.md", VIEWER, tmp));
    }

    @Test
    void theSiteIsBuiltFromMockData(@TempDir Path tmp) throws IOException {
        Path out = Files.createDirectories(tmp.resolve("out"));
        outWith(out, "mock");
        Path site = tmp.resolve("_site");
        Site.build(VIEWER, out, site);
        assertTrue(Files.exists(site.resolve("index.html")));
        assertTrue(Files.exists(site.resolve("transactions.json")));
        assertTrue(Files.exists(site.resolve("wrapped.json")));
    }

    @Test
    void theSiteRefusesYourPrivateData(@TempDir Path tmp) throws IOException {
        Path out = Files.createDirectories(tmp.resolve("out"));
        outWith(out, "private");
        assertThrows(IllegalStateException.class, () -> Site.build(VIEWER, out, tmp.resolve("_site")),
                "private data must never be published");
    }
}
