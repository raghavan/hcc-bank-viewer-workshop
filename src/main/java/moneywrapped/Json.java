package moneywrapped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** One shared JSON reader and writer (Jackson). Done for you. */
public final class Json {
    private Json() {}

    public static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void write(Path path, Object value) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, MAPPER.writeValueAsString(value) + "\n", StandardCharsets.UTF_8);
    }
}
