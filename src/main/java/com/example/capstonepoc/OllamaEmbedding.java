package com.example.capstonepoc;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for generating text embeddings using Ollama.
 * Uses the OllamaEmbeddingModel bean configured in OllamaConfig.
 */
@Service
public class OllamaEmbedding {
  private static final Logger logger = LoggerFactory.getLogger(OllamaEmbedding.class);
  private static final int EXPECTED_VECTOR_LENGTH = 768;

  @Autowired
  private OllamaEmbeddingModel embeddingModel;

  /**
   * Gets embedding vector for the given text.
   *
   * @param text the text to embed
   * @return float array representing the embedding (768 dimensions) or empty array on error
   */
  public float[] getEmbedding(String text) {
    try {
      TextSegment textSegment = TextSegment.from(text);
      Response<Embedding> response = embeddingModel.embed(textSegment);
      Embedding embedding = response.content();
      float[] vector = embedding.vector();
      if (vector.length == EXPECTED_VECTOR_LENGTH) {
        logger.debug("Successfully generated embedding for text of length: {}", text.length());
        return vector;
      } else {
        logger.warn("Unexpected vector length: {}, expected: {}", vector.length, EXPECTED_VECTOR_LENGTH);
      }
    } catch (Exception e) {
      logger.error("FAILED: Ensure Ollama is running and 'nomic-embed-text' is pulled.", e);
    }
    return new float[0];
  }

}