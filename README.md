# Building RAG Applications with Spring AI: A Java Developer's Guide
Learn to build robust RAG (Retrieval-Augmented Generation) applications using Spring AI and Java.


## Technologies Used
- Spring Boot
- Spring AI
- Java
- Maven

## Prerequisites
- JDK 17 or higher
- Maven 3.6 or higher

## Getting Started
1. Clone the repository:
   ```bash
   git clone git@github.com:hhimanshu/springai-rag.git
   cd springai-rag
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

   Ensure you have Maven installed and configured on your system.

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Running the Ingestor
To ingest data into the application, use the provided script:
```bash
chmod +x scripts/ingest
./scripts/ingest <number_of_records>
```
where `<number_of_records>` is the number of records you want to ingest into the application. If you leave it blank, the script will ingest all available records.

## Resources
- Chapter 3. Project Setup
  - [Spring AI Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html#page-title)
  - [Deepwiki Spring AI Home](https://deepwiki.com/spring-projects/spring-ai)
- Chapter 4. Vector Database Implementation
  - [How can I set chroma db settings in application.yml?](https://deepwiki.com/search/how-can-i-set-chroma-db-settin_ba222f6f-b671-4cb8-acde-f5f6c0fc8849)
  - [What is the default tenant and database used with VectorStore?](https://deepwiki.com/search/what-is-the-default-tenant-and_dbc9aa19-8eca-4f5f-a14c-2d53056df04c)
  - [Understanding Vector Stores](https://deepwiki.com/spring-projects/spring-ai/4.1-understanding-vector-stores)
  - [Vector Store Implementations](https://deepwiki.com/spring-projects/spring-ai/4.2-vector-store-implementations)
  - [Configuration Patterns](https://deepwiki.com/spring-projects/spring-ai/4.2-vector-store-implementations#configuration-patterns)
  - [ChromaDB Admin Github Page](https://github.com/flanker/chromadb-admin)
  - [Airline Reviews Dataset](https://www.kaggle.com/datasets/juhibhojani/airline-reviews)
- Chapter 5. Building the RAG Pipeline
  - [Vector Similarity Search Principles](https://deepwiki.com/spring-projects/spring-ai/4.1-understanding-vector-stores#vector-similarity-search-principles)
- Chapter 6. Conclusion and Next Steps
  - [Chunking Strategies for RAG Applications](https://community.databricks.com/t5/technical-blog/the-ultimate-guide-to-chunking-strategies-for-rag-applications/ba-p/113089)
