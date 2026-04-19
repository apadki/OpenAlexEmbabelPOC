# Capstone POC - Spring Boot Application

A Spring Boot application that integrates LangChain4j with Qdrant vector database and Ollama for text embeddings.

## Features

- **Spring Boot 3.2.4** - Modern Spring Boot framework with auto-configuration
- **LangChain4j Integration** - Seamless integration with LangChain4j for AI/ML capabilities
- **Qdrant Vector Database** - High-performance vector database for semantic search
- **Ollama Embeddings** - Local embedding generation using the nomic-embed-text model
- **REST API** - RESTful API for embedding generation
- **Spring Configuration** - Externalized configuration via `application.properties`

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for running Qdrant)
- Ollama (for embedding generation)

## Setup Instructions

### 1. Start Qdrant Vector Database

```bash
docker run -d \
  --name qdrant \
  -p 6334:6334 \
  -p 6333:6333 \
  qdrant/qdrant:latest
```

### 2. Start Ollama Service

```bash
ollama serve
```

In another terminal, pull the embedding model:
```bash
ollama pull nomic-embed-text
```

### 3. Build the Application

```bash
./mvnw clean install
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
```bash
GET /api/embeddings/health
```

Response:
```json
{
  "status": "UP",
  "message": "Embedding service is running"
}
```

### Generate Embedding
```bash
POST /api/embeddings/generate
Content-Type: application/json

{
  "text": "Your text to embed here"
}
```

Response:
```json
{
  "success": true,
  "text": "Your text to embed here",
  "embedding_length": 768,
  "embedding": [0.123, -0.456, ...]
}
```

## Configuration

Edit `src/main/resources/application.properties` to customize:

```properties
# Server
server.port=8080

# Qdrant
qdrant.host=localhost
qdrant.port=6334
qdrant.collection-name=default-collection
qdrant.dimension=768

# Ollama
ollama.base-url=http://localhost:11434
ollama.model-name=nomic-embed-text
ollama.timeout-seconds=60

# Logging
logging.level.root=INFO
logging.level.com.example.capstonepoc=DEBUG
```

## Project Structure

```
src/main/java/com/example/capstonepoc/
├── CapstonePocApplication.java       # Spring Boot application entry point
├── OllamaEmbedding.java             # Embedding service
├── config/
│   ├── VectorDBConfig.java          # Qdrant configuration
│   └── OllamaConfig.java            # Ollama configuration
└── controller/
    └── EmbeddingController.java      # REST API endpoints

src/main/resources/
└── application.properties            # Application configuration
```

## Running Tests

```bash
./mvnw test
```

Run specific test:
```bash
./mvnw test -Dtest=OllamaEmbeddingTest
```

## Spring Boot Features Used

- **@SpringBootApplication** - Enables auto-configuration and component scanning
- **@Configuration** - Defines configuration classes for beans
- **@Bean** - Creates managed beans for Spring container
- **@Service** - Marks classes as service layer components
- **@RestController** - Combines @Controller and @ResponseBody
- **@Autowired** - Dependency injection
- **@Value** - Property value injection from application.properties

## Building for Production

```bash
./mvnw clean package
java -jar target/capstonepoc-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Qdrant Connection Issues
- Ensure Qdrant is running: `docker ps | grep qdrant`
- Check ports: `lsof -i :6334`

### Ollama Connection Issues
- Ensure Ollama service is running
- Verify the model is pulled: `ollama list`
- Check base URL in `application.properties`

### Embedding Generation Fails
- Verify Ollama is accessible: `curl http://localhost:11434/api/tags`
- Check model name matches in configuration
- Review logs: `logging.level.com.example.capstonepoc=DEBUG`

## License

Apache License 2.0

