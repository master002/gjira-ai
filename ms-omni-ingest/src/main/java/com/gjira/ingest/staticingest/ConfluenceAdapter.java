package com.gjira.ingest.staticingest;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Fetches Confluence pages by ID. Extracts HTML body → plain text.
 * Set gjira.confluence.enabled=true and base-url, token.
 */
@Component
@ConditionalOnProperty(prefix = "gjira.confluence", name = "enabled", havingValue = "true")
public class ConfluenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConfluenceAdapter.class);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String token;

    public ConfluenceAdapter(
            @Value("${gjira.confluence.base-url}") String baseUrl,
            @Value("${gjira.confluence.token}") String token
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.token = token;
    }

    public ConfluencePage fetchPage(String pageId) {
        String url = baseUrl + "wiki/rest/api/content/" + pageId + "?expand=body.storage,version";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        if (response.getStatusCode() != HttpStatusCode.valueOf(200) || response.getBody() == null) {
            throw new RuntimeException("Confluence fetch failed: " + response.getStatusCode());
        }

        JsonNode body = response.getBody();
        String html = null;
        JsonNode storage = body.path("body").path("storage");
        if (!storage.isMissingNode()) {
            html = storage.path("value").asText(null);
        }
        if (html == null) {
            JsonNode view = body.path("body").path("view");
            if (!view.isMissingNode()) {
                html = view.path("value").asText(null);
            }
        }
        if (html == null) html = "";

        String plainText = stripHtml(html);
        String title = body.path("title").asText("");
        String versionWhen = body.path("version").path("when").asText(null);

        return new ConfluencePage(pageId, title, plainText, versionWhen);
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return HTML_TAG.matcher(html).replaceAll(" ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record ConfluencePage(String pageId, String title, String plainText, String versionWhen) {}
}
