package com.springai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@SpringBootApplication(scanBasePackages = "com.springai.rag")
public class Ingestor {

    private static final int AIRLINE_NAME_COL = 1;
    private static final int OVERALL_RATING_COL = 2;
    private static final int REVIEW_TITLE_COL = 3;
    private static final int REVIEW_COL = 6;
    private static final int SEAT_TYPE_COL = 9;
    private static final int ROUTE_COL = 10;
    private static final int RECOMMENDED_COL = 19;

    public static void main(String[] args) {
        System.out.println("=== RAG Data Ingestor ===\n");

        Integer limit = null;
        if (args.length > 0 && !args[0].isEmpty()) {
            try {
                limit = Integer.parseInt(args[0]);
                System.out.println("Ingesting " + limit + " records...");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Usage: ./scripts/ingest [number]");
                System.exit(1);
            }
        } else {
            System.out.println("Ingesting all records...");
        }

        ApplicationContext context = SpringApplication.run(Ingestor.class, args);
        VectorStore vectorStore = context.getBean(VectorStore.class);

        Ingestor ingestor = new Ingestor();
        ingestor.ingestDocuments(vectorStore, limit);

        System.exit(0);
    }

    private void ingestDocuments(VectorStore vectorStore, Integer limit) {
        try {
            System.out.println("\nStep 1: Reading CSV file...");
            List<Document> documents = readCsvFile("airline_review.csv", limit);
            System.out.println("  ✓ Read " + documents.size() + " reviews from CSV");

            System.out.println("\nStep 2: Splitting documents into chunks...");
            List<Document> chunks = splitIntoChunks(documents);
            System.out.println("  ✓ Split into " + chunks.size() + " chunks");

            System.out.println("\nStep 3: Storing in vector database...");
            storeInVectorDatabase(vectorStore, chunks);
            System.out.println("  ✓ Successfully stored all chunks in ChromaDB");

            System.out.println("\n✅ Ingestion complete!");

        } catch (Exception e) {
            System.err.println("❌ Error during ingestion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Document> readCsvFile(String csvPath, Integer limit) throws Exception {
        List<Document> documents = new ArrayList<>();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvPath);

        if (inputStream == null) {
            throw new Exception("CSV file not found: " + csvPath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            int count = 0;
            int rowIndex = 0; // Zero-based index for CSV rows (excluding header)

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                if (limit != null && count >= limit) {
                    break;
                }

                Document doc = createDocument(parseCsvRow(line), rowIndex);
                if (doc != null) {
                    documents.add(doc);
                    count++;

                    if (count % 50 == 0) {
                        System.out.println("  Processing row " + count + "...");
                    }
                }
                rowIndex++; // Increment regardless of whether doc was created
            }
        }

        return documents;
    }

    private Document createDocument(String[] columns, int rowIndex) {
        if (columns.length < 20) {
            return null;
        }

        String reviewText = cleanText(columns[REVIEW_COL]);
        if (reviewText.isEmpty()) {
            return null;
        }

        String reviewTitle = cleanText(columns[REVIEW_TITLE_COL]);
        String fullContent = reviewTitle.isEmpty() ? reviewText : reviewTitle + "\n\n" + reviewText;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", "id-" + rowIndex);
        metadata.put("airline_name", cleanText(columns[AIRLINE_NAME_COL]));
        metadata.put("overall_rating", parseRating(columns[OVERALL_RATING_COL]));
        metadata.put("review_title", reviewTitle);
        metadata.put("seat_type", cleanText(columns[SEAT_TYPE_COL]));
        metadata.put("route", cleanText(columns[ROUTE_COL]));
        metadata.put("recommended", parseRecommendation(columns[RECOMMENDED_COL]));

        return new Document(fullContent, metadata);
    }

    private List<Document> splitIntoChunks(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(500, 200, 10, 10000, true);
        List<Document> chunks = new ArrayList<>();

        for (Document doc : documents) {
            List<Document> docChunks = splitter.apply(List.of(doc));
            chunks.addAll(docChunks);
        }

        return chunks;
    }

    private void storeInVectorDatabase(VectorStore vectorStore, List<Document> chunks) {
        int batchSize = 50;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<Document> batch = chunks.subList(i, end);

            System.out.println("  Storing batch " + (i / batchSize + 1) +
                    " (" + batch.size() + " documents)...");
            vectorStore.add(batch);
        }
    }

    private String[] parseCsvRow(String csvRow) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < csvRow.length(); i++) {
            char c = csvRow.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }

    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.trim()
                .replaceAll("^\"|\"$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Double parseRating(String rating) {
        try {
            String cleaned = cleanText(rating);
            return cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Boolean parseRecommendation(String recommendation) {
        String cleaned = cleanText(recommendation).toLowerCase();
        return "yes".equals(cleaned);
    }
}