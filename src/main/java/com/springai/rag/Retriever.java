package com.springai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = "com.springai.rag")
public class Retriever {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    public static void main(String[] args) {
        System.out.println("=== RAG Document Retriever ===\n");

        if (args.length == 0) {
            System.out.println("Usage: ./scripts/retrieve \"your query\"");
            System.exit(1);
        }

        StringBuilder queryBuilder = new StringBuilder();

        for (String arg : args) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" ");
            }
            queryBuilder.append(arg);
        }

        String query = queryBuilder.toString().trim();

        ApplicationContext context = SpringApplication.run(Retriever.class, args);
        VectorStore vectorStore = context.getBean(VectorStore.class);

        Retriever retriever = new Retriever();
        retriever.retrieveBasic(query, vectorStore);

        System.exit(0);
    }

    private void retrieveBasic(String query, VectorStore vectorStore) {
        System.out.println("Query: \"" + query + "\"\n");
        System.out.println("Step 1: Building search request...");

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();

        System.out.println("  ✓ Top K: " + DEFAULT_TOP_K);
        System.out.println("  ✓ Similarity threshold: " + DEFAULT_SIMILARITY_THRESHOLD);

        System.out.println("\nStep 2: Executing vector search...");
        List<Document> results = executeSearch(vectorStore, searchRequest);

        System.out.println("\nStep 3: Displaying results...");
        displayResults(results);
    }

    private List<Document> executeSearch(VectorStore vectorStore, SearchRequest searchRequest) {
        try {
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            if (results != null) {
                System.out.println("  ✓ Found " + results.size() + " matching documents");
                return results;
            } else {
                System.out.println("  ✓ Found 0 matching documents (null result)");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("  ❌ Search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void displayResults(List<Document> documents) {
        if (documents.isEmpty()) {
            System.out.println("\n❌ No matching documents found.");
            return;
        }

        System.out.println("\n" + "=".repeat(60));

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            System.out.println("\nResult #" + (i + 1) + ":");
            System.out.println("-".repeat(40));

            Map<String, Object> metadata = doc.getMetadata();
            System.out.println("Airline: " + metadata.getOrDefault("airline_name", "Unknown"));
            System.out.println("Rating: " + metadata.getOrDefault("overall_rating", 0.0) + "/10");
            System.out.println("Seat Type: " + metadata.getOrDefault("seat_type", "Not specified"));
            System.out.println("Route: " + metadata.getOrDefault("route", "Not specified"));
            System.out.println("Recommended: " +
                    (Boolean.TRUE.equals(metadata.get("recommended")) ? "Yes" : "No"));

            String reviewTitle = (String) metadata.get("review_title");
            if (reviewTitle != null && !reviewTitle.isEmpty()) {
                System.out.println("Title: \"" + reviewTitle + "\"");
            }

            System.out.println("\nFull Review Text:");
            String text = doc.getText();
            // Show full document for teaching purposes - no truncation
            System.out.println(text != null ? text : "(No content available)");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("\n✅ Search complete!");
    }
}