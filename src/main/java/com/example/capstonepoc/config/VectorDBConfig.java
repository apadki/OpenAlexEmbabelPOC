package com.example.capstonepoc.config;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Qdrant Vector Database initialization.
 * Provides a Spring Bean for QdrantEmbeddingStore with configurable
 * host, port, collection name, and dimension.
 */
@Configuration
public class VectorDBConfig {
  private static final Logger logger = LoggerFactory.getLogger(VectorDBConfig.class);

  @Value("${qdrant.host:localhost}")
  private String qdrantHost;

  @Value("${qdrant.port:6334}")
  private int qdrantPort;

  @Value("${qdrant.collection-name:default-collection}")
  private String collectionName;

  @Value("${qdrant.dimension:768}")
  private int dimension;

  /**
   * Creates and initializes a QdrantEmbeddingStore bean.
   *
   * @return QdrantEmbeddingStore configured with application properties
   */
  @Bean
  public QdrantEmbeddingStore qdrantEmbeddingStore() {
    return initializeQdrantStore(collectionName, dimension);
  }

  /**
   * Initializes Qdrant store with specified collection name and dimension.
   *
   * @param collectionName the name of the collection
   * @param dimension the dimension of the vectors
   * @return initialized QdrantEmbeddingStore
   */
  public QdrantEmbeddingStore initializeQdrantStore(String collectionName, int dimension) {
    // 1. Connect to the Qdrant gRPC client (faster for 100k records)
    QdrantClient client = new QdrantClient(
      QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
    );

    try {
      // 2. Check if collection exists; if not, create it
      boolean exists = client.listCollectionsAsync().get()
        .stream()
        .anyMatch(c -> c.equals(collectionName));

      if (!exists) {
        client.createCollectionAsync(collectionName,
          VectorParams.newBuilder()
            .setDistance(Distance.Cosine) // Recommended for OpenAlex text
            .setSize(dimension)           // Must match your EmbeddingModel
            .build()
        ).get();
        logger.info("Collection created: {}", collectionName);
      } else {
        logger.info("Collection already exists: {}", collectionName);
      }
    } catch (Exception e) {
      logger.error("Failed to initialize Qdrant collection", e);
      throw new RuntimeException("Failed to initialize Qdrant collection", e);
    }

    // 3. Return the LangChain4j EmbeddingStore wrapper
    return QdrantEmbeddingStore.builder()
      .host(qdrantHost)
      .port(qdrantPort)
      .collectionName(collectionName)
      .build();
  }
}

