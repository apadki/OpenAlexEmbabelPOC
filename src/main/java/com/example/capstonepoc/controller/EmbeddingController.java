package com.example.capstonepoc.controller;

import com.example.capstonepoc.OllamaEmbedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for embedding operations.
 * Provides endpoints for generating text embeddings.
 */
@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {
  private static final Logger logger = LoggerFactory.getLogger(EmbeddingController.class);

  @Autowired
  private OllamaEmbedding ollamaEmbedding;

  /**
   * Health check endpoint.
   *
   * @return status response
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Embedding service is running");
    return ResponseEntity.ok(response);
  }

  /**
   * Generate embedding for given text.
   *
   * @param request containing the text to embed
   * @return embedding response with vector and metadata
   */
  @PostMapping("/generate")
  public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody EmbeddingRequest request) {
    Map<String, Object> response = new HashMap<>();

    try {
      float[] embedding = ollamaEmbedding.getEmbedding(request.getText());

      if (embedding.length == 0) {
        logger.warn("Failed to generate embedding for text: {}", request.getText());
        response.put("success", false);
        response.put("error", "Failed to generate embedding. Ensure Ollama is running.");
        return ResponseEntity.badRequest().body(response);
      }

      response.put("success", true);
      response.put("text", request.getText());
      response.put("embedding_length", embedding.length);
      response.put("embedding", embedding);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error generating embedding", e);
      response.put("success", false);
      response.put("error", e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }

  /**
   * Request body for embedding generation.
   */
  public static class EmbeddingRequest {
    private String text;

    public EmbeddingRequest() {}

    public EmbeddingRequest(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }
  }
}

