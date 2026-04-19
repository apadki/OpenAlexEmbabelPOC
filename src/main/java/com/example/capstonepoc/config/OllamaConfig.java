package com.example.capstonepoc.config;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Ollama Embedding Model as a Spring Bean.
 * Provides automatic initialization of the embedding model with configurable properties.
 */
@Configuration
public class OllamaConfig {

  @Value("${ollama.base-url:http://localhost:11434}")
  private String baseUrl;

  @Value("${ollama.model-name:nomic-embed-text}")
  private String modelName;

  @Value("${ollama.timeout-seconds:60}")
  private int timeoutSeconds;

  /**
   * Creates an OllamaEmbeddingModel bean.
   *
   * @return configured OllamaEmbeddingModel
   */
  @Bean
  public OllamaEmbeddingModel ollamaEmbeddingModel() {
    return OllamaEmbeddingModel.builder()
      .baseUrl(baseUrl)
      .modelName(modelName)
      .timeout(Duration.ofSeconds(timeoutSeconds))
      .logRequests(true)  // Helpful for debugging your POC
      .logResponses(true)
      .build();
  }
}

