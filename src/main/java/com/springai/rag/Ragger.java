package com.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = "com.springai.rag")
public class Ragger {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    public static void main(String[] args) {
        System.out.println("=== Spring AI RAG System ===\n");

        if (args.length == 0) {
            System.out.println("Usage: ./scripts/rag \"your question\"");
            System.exit(1);
        }

        StringBuilder questionBuilder = new StringBuilder();

        for (String arg : args) {
            if (questionBuilder.length() > 0) {
                questionBuilder.append(" ");
            }
            questionBuilder.append(arg);
        }

        String question = questionBuilder.toString().trim();

        ApplicationContext context = SpringApplication.run(Ragger.class, args);
        VectorStore vectorStore = context.getBean(VectorStore.class);
        ChatClient.Builder chatClientBuilder = context.getBean(ChatClient.Builder.class);

        Ragger ragger = new Ragger();
        ragger.processQuestion(question, vectorStore, chatClientBuilder);

        System.exit(0);
    }

    private void processQuestion(String question, VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder) {
        System.out.println("Question: \"" + question + "\"\n");

        // Configure the retrieval augmentation advisor
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                                .topK(DEFAULT_TOP_K)
                                .vectorStore(vectorStore)
                                .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        try {
            // Use Spring AI's built-in RAG - everything happens automatically!
            String response = chatClientBuilder.build()
                    .prompt()
                    .user(question)
                    .advisors(retrievalAugmentationAdvisor)
                    .call()
                    .content();

            // Display response
            System.out.println("=".repeat(60));
            System.out.println("ANSWER:");
            System.out.println("=".repeat(60));
            System.out.println(response);
            System.out.println("=".repeat(60));
            System.out.println("\n✅ Spring AI RAG processing complete!");

        } catch (Exception e) {
            System.err.println("Error in RAG processing: " + e.getMessage());
            System.exit(1);
        }
    }
}