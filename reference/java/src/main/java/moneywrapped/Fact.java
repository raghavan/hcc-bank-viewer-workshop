package moneywrapped;

import java.util.List;
import java.util.Map;

/**
 * One Money Wrapped fact, computed by Java from your transactions. Done for you.
 *
 * @param id     a short name the AI must cite, like "top_merchant"
 * @param label  a title for people, like "Top merchant"
 * @param text   one plain sentence with the numbers in it
 * @param values every number in the sentence, written exactly as people see it ("$1,956.42", "24")
 * @param data   the same facts as raw values (cents, counts) for tests and the viewer
 */
public record Fact(String id, String label, String text, List<String> values, Map<String, Object> data) {}
