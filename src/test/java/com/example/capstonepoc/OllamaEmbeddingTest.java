package com.example.capstonepoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest // This starts the Spring context and finds your @Beans
class OllamaEmbeddingTest {

  @Autowired
  private OllamaEmbedding ollamaEmbedding; // Let Spring provide the fully initialized bean

  @Test
  @DisplayName("Should return vector of length 768 for valid embedding")
  void testGetEmbeddingReturnsCorrectVectorLength() {
    String testText = "This is a test embedding";
    float[] result = ollamaEmbedding.getEmbedding(testText);

    assertNotNull(result);
    assertEquals(768, result.length);
  }
}