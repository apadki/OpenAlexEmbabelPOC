package com.example.capstonepoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for Capstone POC.
 * This application integrates LangChain4j with Qdrant vector database
 * and Ollama for embeddings.
 */
@SpringBootApplication
public class CapstonePocApplication {

  public static void main(String[] args) {
    SpringApplication.run(CapstonePocApplication.class, args);
  }
}

