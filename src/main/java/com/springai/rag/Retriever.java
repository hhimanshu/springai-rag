package com.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.*;

@SpringBootApplication(scanBasePackages = "com.springai.rag")
public class Retriever {
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;
    
    public static void main(String[] args) {
        System.out.println("=== RAG Document Retriever ===\n");
        
        if (args.length == 0) {
            System.out.println("Usage: ./scripts/retrieve \"your query\" [--smart]");
            System.out.println("  --smart : Enable query understanding with filters");
            System.exit(1);
        }
        
        // Parse arguments - handle case where query might be split across multiple args
        boolean smartMode = false;
        StringBuilder queryBuilder = new StringBuilder();
        
        for (String arg : args) {
            if ("--smart".equals(arg)) {
                smartMode = true;
            } else {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append(" ");
                }
                queryBuilder.append(arg);
            }
        }
        
        String query = queryBuilder.toString().trim();
        
        ApplicationContext context = SpringApplication.run(Retriever.class, args);
        VectorStore vectorStore = context.getBean(VectorStore.class);
        ChatClient.Builder chatClientBuilder = context.getBean(ChatClient.Builder.class);
        
        Retriever retriever = new Retriever();
        
        if (smartMode) {
            System.out.println("Mode: Smart search (with query understanding)\n");
            retriever.retrieveSmart(query, vectorStore, chatClientBuilder);
        } else {
            System.out.println("Mode: Basic similarity search\n");
            retriever.retrieveBasic(query, vectorStore);
        }
        
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
    
    private void retrieveSmart(String query, VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        System.out.println("Original Query: \"" + query + "\"\n");
        
        System.out.println("Step 1: Enhancing query with AI...");
        Map<String, Object> enhancement = enhanceQuery(query, chatClientBuilder);
        String enhancedQuery = (String) enhancement.get("query");
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) enhancement.get("filters");
        
        System.out.println("  ✓ Enhanced query: \"" + enhancedQuery + "\"");
        if (filters != null && !filters.isEmpty()) {
            System.out.println("  ✓ Extracted filters: " + filters);
        }
        
        System.out.println("\nStep 2: Building search request with filters...");
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
            .query(enhancedQuery)
            .topK(DEFAULT_TOP_K)
            .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);
        
        if (filters != null && !filters.isEmpty()) {
            String filterExpression = buildFilterExpression(filters);
            System.out.println("  ✓ Filter expression: " + filterExpression);
            requestBuilder.filterExpression(filterExpression);
        }
        
        SearchRequest searchRequest = requestBuilder.build();
        
        System.out.println("\nStep 3: Executing smart search...");
        List<Document> results = executeSearch(vectorStore, searchRequest);
        
        System.out.println("\nStep 4: Displaying results...");
        displayResults(results);
    }
    
    private Map<String, Object> enhanceQuery(String query, ChatClient.Builder chatClientBuilder) {
        ChatClient chatClient = chatClientBuilder.build();
        
        String prompt = """
            Analyze this airline review query and extract:
            1. Enhanced search terms (focus on key concepts)
            2. Any filters (airline name, rating range, seat type, route)
            
            Query: "%s"
            
            Respond in this exact format:
            ENHANCED: <enhanced query terms>
            AIRLINE: <airline name or NONE>
            MIN_RATING: <minimum rating or NONE>
            SEAT_TYPE: <seat type or NONE>
            """.formatted(query);
        
        String response = chatClient.prompt().user(prompt).call().content();
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> filters = new HashMap<>();
        
        if (response == null) {
            result.put("query", query); // fallback to original query
            result.put("filters", filters);
            return result;
        }
        
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith("ENHANCED:")) {
                result.put("query", line.substring(9).trim());
            } else if (line.startsWith("AIRLINE:") && !line.contains("NONE")) {
                filters.put("airline_name", line.substring(8).trim());
            } else if (line.startsWith("MIN_RATING:") && !line.contains("NONE")) {
                try {
                    filters.put("min_rating", Double.parseDouble(line.substring(11).trim()));
                } catch (NumberFormatException e) {
                    // Ignore invalid ratings
                }
            } else if (line.startsWith("SEAT_TYPE:") && !line.contains("NONE")) {
                filters.put("seat_type", line.substring(10).trim());
            }
        }
        
        result.put("filters", filters);
        return result;
    }
    
    private String buildFilterExpression(Map<String, Object> filters) {
        List<String> expressions = new ArrayList<>();
        
        if (filters.containsKey("airline_name")) {
            expressions.add("airline_name == '" + filters.get("airline_name") + "'");
        }
        
        if (filters.containsKey("min_rating")) {
            expressions.add("overall_rating >= " + filters.get("min_rating"));
        }
        
        if (filters.containsKey("seat_type")) {
            expressions.add("seat_type == '" + filters.get("seat_type") + "'");
        }
        
        return String.join(" && ", expressions);
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