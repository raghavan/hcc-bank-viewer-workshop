package moneywrapped;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny CSV reader. Done for you, so you can spend your time on what each bank does
 * differently. Each row comes back as a map from header name to cell text.
 */
public final class Csv {
    private Csv() {}

    /** Split one line into cells, keeping "quoted, commas" together. */
    public static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cur.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    /**
     * The lines that hold data: skip the first skipLines lines (junk above the header),
     * blank lines and lines that start with #. The first line left is the header.
     */
    public static List<String> dataLines(Path path, int skipLines) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> kept = new ArrayList<>();
        for (String line : lines.subList(Math.min(skipLines, lines.size()), lines.size())) {
            String s = line.replace("﻿", "");
            if (!s.isBlank() && !s.stripLeading().startsWith("#")) kept.add(s);
        }
        return kept;
    }

    /** Read a CSV file into one map per data row, in file order. */
    public static List<Map<String, String>> read(Path path, int skipLines) throws IOException {
        List<String> lines = dataLines(path, skipLines);
        if (lines.isEmpty()) throw new IOException(path + " has no header line");
        List<String> header = split(lines.get(0)).stream().map(String::trim).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = split(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) row.put(header.get(i), i < cells.size() ? cells.get(i) : "");
            rows.add(row);
        }
        return rows;
    }

    /** A cell by column name, failing with a clear message when the column is missing. */
    public static String cell(Map<String, String> row, String column) {
        String v = row.get(column);
        if (v == null) throw new IllegalArgumentException("No column named \"" + column + "\". Columns are " + row.keySet());
        return v;
    }
}
