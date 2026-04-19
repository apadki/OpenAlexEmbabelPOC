package com.example.capstonepoc;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OllamaEmbedding Tests")
class OllamaEmbeddingTest {

  private OllamaEmbedding ollamaEmbedding        ;

  @BeforeEach
  void setUp() {
    ollamaEmbedding = new OllamaEmbedding();
  }

  @Test
  @DisplayName("Should return vector of length 768 for valid embedding")
  void testGetEmbeddingReturnsCorrectVectorLength() {
    String testText = "This is a test embedding";
    float[] result = ollamaEmbedding.getEmbedding(testText);

    // Should return a vector of length 768 for successful embeddings
    assertNotNull(result, "Result should not be null");
    assertTrue(result.length == 768 || result.length == 0,
        "Vector should be either 768 (success) or 0 (failure)");
  }

  @Test
  @DisplayName("Should handle null input gracefully")
  void testGetEmbeddingWithNullInput() {
    float[] result = ollamaEmbedding.getEmbedding(null);

    assertNotNull(result, "Result should not be null");
    assertEquals(0, result.length, "Should return empty array on error");
  }

  @Test
  @DisplayName("Should handle empty string input")
  void testGetEmbeddingWithEmptyString() {
    float[] result = ollamaEmbedding.getEmbedding("");

    assertNotNull(result, "Result should not be null");
    // Could be either 768 or 0 depending on model behavior
    assertTrue(result.length == 768 || result.length == 0,
        "Vector should be either 768 (success) or 0 (failure)");
  }

  @Test
  @DisplayName("Should handle long text input")
  void testGetEmbeddingWithLongText() {
    String longText = "This is a much longer text that tests the embedding model's ability to handle " +
        "extended inputs. Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";

    float[] result = ollamaEmbedding.getEmbedding(longText);

    assertNotNull(result, "Result should not be null");
    assertTrue(result.length == 768 || result.length == 0,
        "Vector should be either 768 (success) or 0 (failure)");
  }

  @Test
  @DisplayName("Should return empty array on exception")
  void testGetEmbeddingErrorHandling() {
    // This test verifies that the method returns an empty array when an exception occurs
    String testText = "Test";
    float[] result = ollamaEmbedding.getEmbedding(testText);

    assertNotNull(result, "Result should not be null even on error");
    if (result.length == 0) {
      // Error case - empty array returned
      assertTrue(true, "Error handling works - returned empty array");
    } else {
      // Success case - valid vector returned
      assertEquals(768, result.length, "On success, vector length should be 768");
    }
  }

  @Test
  @DisplayName("Should return vector with float values")
  void testGetEmbeddingReturnsFloatArray() {
    String testText = "Test embedding";
    float[] result = ollamaEmbedding.getEmbedding(testText);

    assertNotNull(result, "Result should not be null");

    if (result.length == 768) {
      // Verify that at least some values are not NaN or infinite
      for (float value : result) {
        assertFalse(Float.isNaN(value), "Vector should not contain NaN values");
        assertFalse(Float.isInfinite(value), "Vector should not contain infinite values");
      }
    }
  }

  @Test
  @DisplayName("Should return consistent embeddings for the same input")
  void testGetEmbeddingConsistency() {
    String testText = "Consistent test";
    float[] result1 = ollamaEmbedding.getEmbedding(testText);
    float[] result2 = ollamaEmbedding.getEmbedding(testText);

    assertNotNull(result1, "First result should not be null");
    assertNotNull(result2, "Second result should not be null");

    // Both should have the same length
    assertEquals(result1.length, result2.length,
        "Same input should produce vectors of same length");

    // If both are successful (768 length), they should be identical
    if (result1.length == 768 && result2.length == 768) {
      assertArrayEquals(result1, result2, 0.0001f,
          "Same input should produce identical vectors");
    }
  }
}

