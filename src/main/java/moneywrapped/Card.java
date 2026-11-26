package moneywrapped;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * One Wrapped card: a short title and caption that tell a story about one or more facts.
 * Cards come from a template (no AI), the fake AI, or Claude. Done for you.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Card {
    /** The ids of the facts this card is about. The grounding check reads these. */
    @JsonProperty("fact_ids")
    public List<String> factIds = new ArrayList<>();

    public String title;
    public String caption;

    /** Filled in by our code, never by the AI: template, mock_ai or claude. */
    public String source;

    /** Filled in by the grounding check when it rejects a card. */
    public String reason;

    public Card() {}

    public Card(String source, String title, String caption, String... factIds) {
        this.source = source;
        this.title = title;
        this.caption = caption;
        this.factIds = new ArrayList<>(List.of(factIds));
    }
}
