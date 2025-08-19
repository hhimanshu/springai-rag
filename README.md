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

 ## About Data
 The CSV data is downloaded from [Kaggle](https://www.kaggle.com/datasets/juhibhojani/airline-reviews)