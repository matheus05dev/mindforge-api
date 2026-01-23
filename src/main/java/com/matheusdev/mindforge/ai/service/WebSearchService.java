package com.matheusdev.mindforge.ai.service;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebSearchService {

    private final WebSearchEngine webSearchEngine;

    public WebSearchService(@Value("${tavily.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            this.webSearchEngine = null;
            log.warn("Tavily API Key is missing. Web search will be disabled.");
        } else {
            this.webSearchEngine = TavilyWebSearchEngine.builder()
                    .apiKey(apiKey)
                    .build();
            log.info("WebSearchService initialized with Tavily.");
        }
    }

    public List<String> search(String query) {
        if (webSearchEngine == null) {
            log.warn("Web search disabled. Skipping query: {}", query);
            return Collections.emptyList();
        }

        try {
            log.info("Searching web for: {}", query);
            WebSearchResults results = webSearchEngine.search(query);

            return results.results().stream()
                    .map(result -> String.format("Title: %s\nURL: %s\nSnippet: %s",
                            result.title(), result.url(), result.content()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error performing web search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
