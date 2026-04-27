package com.example.capstonepoc.config;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VectorDBConfig {
  private static final Logger logger = LoggerFactory.getLogger(VectorDBConfig.class);

  @Value("${qdrant.host:localhost}")
  private String qdrantHost;

  @Value("${qdrant.port:6334}")
  private int qdrantPort;

  @Value("${qdrant.collection-name}")
  private String collectionName;

  @Value("${qdrant.dimension:768}")
  private int dimension;

  @Bean
  public QdrantEmbeddingStore qdrantEmbeddingStore() {
    // 1. Setup the Schema (The DDL)
    setupSchema();

    // 2. Return the Store
    return QdrantEmbeddingStore.builder()
      .host(qdrantHost)
      .port(qdrantPort)
      .collectionName(collectionName)
      .build();
  }

  private void setupSchema() {
    // Use the internal gRPC client
    try (QdrantClient client = new QdrantClient(
      QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build())) {

      List<String> collections = client.listCollectionsAsync().get();
      boolean exists = collections.contains(collectionName);

      if (!exists) {
        logger.info("Executing DDL for collection: {}", collectionName);

        // Create Table (Collection)
        client.createCollectionAsync(collectionName,
          VectorParams.newBuilder()
            .setDistance(Distance.Cosine)
            .setSize(dimension)
            .build()
        ).get();

        // Define Schema Indexes (The DDL fields)
        createPayloadIndex(client, "id", PayloadSchemaType.Keyword);
        createPayloadIndex(client, "primary_topic", PayloadSchemaType.Keyword);
        createPayloadIndex(client, "publisher", PayloadSchemaType.Keyword);
        createPayloadIndex(client, "country", PayloadSchemaType.Keyword);
        createPayloadIndex(client, "pub_year", PayloadSchemaType.Integer);
        createPayloadIndex(client, "cited_by_count", PayloadSchemaType.Integer);
        createPayloadIndex(client, "first_author", PayloadSchemaType.Text);
        createPayloadIndex(client, "institution_names", PayloadSchemaType.Text);

        logger.info("DDL successfully applied to Qdrant.");
      }
    } catch (Exception e) {
      logger.error("Schema setup failed", e);
      throw new RuntimeException(e);
    }
  }

  private void createPayloadIndex(QdrantClient client, String fieldName, PayloadSchemaType type) throws Exception {
    // Standard v1.13.0 signature: 7 arguments
    client.createPayloadIndexAsync(collectionName, fieldName, type, null, true, null, null).get();
  }
}