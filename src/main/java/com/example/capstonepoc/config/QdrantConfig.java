package com.example.capstonepoc.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

  // Using @Value allows you to change these in application.properties later
  @Value("${qdrant.host:localhost}")
  private String host;

  @Value("${qdrant.port:6334}")
  private int port;

  @Bean
  public QdrantClient qdrantClient() {
    // Build the gRPC client
    QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(host, port, false)
      .build();

    // Return the main QdrantClient wrapper
    return new QdrantClient(grpcClient);
  }
}
