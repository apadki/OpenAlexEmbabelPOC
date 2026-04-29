package com.example.capstonepoc;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static java.util.Date.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
// io.qdrant.client.grpc.Points helpers were removed from usage; no direct protobuf imports needed here.
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpsertPoints;
import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import io.qdrant.client.grpc.JsonWithInt.Value; // This is the specific one needed
import io.qdrant.client.ValueFactory;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

@Component
public class OpenAlexIngestionRunner implements CommandLineRunner {

  private final DataSource dataSource;
  private final QdrantClient qdrantClient;
  private final ObjectMapper mapper = new ObjectMapper();
  private final OllamaEmbedding ollamaEmbedding;
  private static final String COLLECTION = "research_works";

  public OpenAlexIngestionRunner(DataSource dataSource, QdrantClient qdrantClient, OllamaEmbedding ollamaEmbedding) {
    this.dataSource = dataSource;
    this.qdrantClient = qdrantClient;
    this.ollamaEmbedding = ollamaEmbedding;
  }

  @Override
  public void run(String... args) throws Exception {
    // Path to your test JSON file on your Mac
    String filePath = "/Users/deerlover/jhu/oa_metadata_100k/W2993480649.json";
    String rawJson = Files.readString(Paths.get(filePath));

    System.out.println("Starting ingestion for test document...");
    processDocument(rawJson);
  }

  public void processDocument(String rawJson) throws Exception {
    JsonNode root = mapper.readTree(rawJson);

    // 1. Abstract Reconstruction (Logic provided by you)
    String plainAbstract = convertInvertedIndex(root.path("abstract_inverted_index"));

    // 2. Data Preparation
    String id = root.path("id").asText();
    String title = root.path("title").asText("");
    String topic = root.path("primary_topic").path("display_name").asText("Unknown");

    // Custom Vector String logic (Logic provided by you)
    String vectorInput = String.format("%s. Topic: %s. %s", title, topic, plainAbstract);
    float[] vector = ollamaEmbedding.getEmbedding(vectorInput);

    // 3. Persist to PostgreSQL
    try (Connection conn = dataSource.getConnection()) {
      saveToPostgres(conn, root, id, title, topic, plainAbstract);
    }

    // 4. Persist to Qdrant
    saveToQdrant(id, topic, root, vector);

    System.out.println("Ingestion Complete for ID: " + id);
  }

  private void saveToPostgres(Connection conn, JsonNode root, String id, String title, String topic, String abstractText) throws SQLException {
    String sql = "INSERT INTO research_works (id, doi, title, abstract, journal_name, publisher, first_author, " +
      "first_author_id, institution_names, countries, primary_topic, concepts, pub_year, pub_date, " +
      "cited_by_count, is_open_access, landing_page_url, pdf_url) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
      "ON CONFLICT (id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, id);
      stmt.setString(2, root.path("doi").asText(null));
      stmt.setString(3, title);
      stmt.setString(4, abstractText);
      stmt.setString(5, root.path("primary_location").path("source").path("display_name").asText(null));
      stmt.setString(6, root.path("primary_location").path("source").path("host_organization_name").asText(null));

      // Author logic
      JsonNode firstAuthor = root.path("authorships").get(0);
      stmt.setString(7, firstAuthor != null ? firstAuthor.path("author").path("display_name").asText(null) : null);
      stmt.setString(8, firstAuthor != null ? firstAuthor.path("author").path("id").asText(null) : null);

      // Institution names (Flattened string)
      String institutions = StreamSupport.stream(root.path("authorships").spliterator(), false)
        .flatMap(a -> StreamSupport.stream(a.path("institutions").spliterator(), false))
        .map(i -> i.path("display_name").asText())
        .distinct().collect(Collectors.joining(", "));
      stmt.setString(9, institutions);
      List<String> countryList = StreamSupport.stream(root.path("authorships").spliterator(), false)
        .flatMap(a -> StreamSupport.stream(a.path("institutions").spliterator(), false))
        .map(i -> i.path("country_code").asText())
        .filter(c -> !c.isEmpty())
        .distinct()
        .collect(Collectors.toList());
      java.sql.Array countryArray = conn.createArrayOf("text", countryList.toArray());

      // Postgres Arrays (countries and concepts)
      stmt.setArray(10, countryArray);
      stmt.setString(11, topic);
      stmt.setArray(12, conn.createArrayOf("text", getArrayFromJson(root.path("concepts"), "display_name")));

      stmt.setInt(13, root.path("publication_year").asInt());
      stmt.setDate(14, java.sql.Date.valueOf(root.path("publication_date").asText("1900-01-01")));
      stmt.setInt(15, root.path("cited_by_count").asInt(0));
      stmt.setBoolean(16, root.path("open_access").path("is_oa").asBoolean(false));
      String url = root.path("primary_location").path("landing_page_url").asText(null);
      if (url != null) {
        stmt.setString(17, url);
      } else {
        stmt.setNull(17, java.sql.Types.VARCHAR);
      }
      stmt.setString(18, root.path("open_access").path("oa_url").asText(null));

      stmt.executeUpdate();
    }
  }

  private void saveToQdrant(String id, String topic, JsonNode root, float[] vector) throws Exception {
    Map<String, Value> payload = new HashMap<>();
    payload.put("id", value(id));
    payload.put("doi", value(root.path("doi").asText("")));
    payload.put("pub_year", value(root.path("publication_year").asInt()));
    payload.put("cited_by_count", value(root.path("cited_by_count").asInt(0)));
    payload.put("primary_topic", value(topic));
    payload.put("journal_name", value(root.path("primary_location").path("source").path("display_name").asText("")));
    payload.put("is_open_access", value(root.path("open_access").path("is_oa").asBoolean(false)));

    // Metadata text fields
    payload.put("publisher", value(root.path("primary_location").path("source").path("host_organization_name").asText("")));

    // Handling the concepts keyword list for Qdrant
    List<String> conceptList = Arrays.asList(getArrayFromJson(root.path("concepts"), "display_name"));
    payload.put("concepts", value(conceptList.stream().map(v -> value(v)).collect(Collectors.toList())));
    String institutions = StreamSupport.stream(root.path("authorships").spliterator(), false)
      .flatMap(a -> StreamSupport.stream(a.path("institutions").spliterator(), false))
      .map(i -> i.path("display_name").asText())
      .distinct().collect(Collectors.joining(", "));
    payload.put("institution_names", value(institutions));

    List<String> countryList = StreamSupport.stream(root.path("authorships").spliterator(), false)
      .flatMap(a -> StreamSupport.stream(a.path("institutions").spliterator(), false))
      .map(i -> i.path("country_code").asText())
      .filter(c -> !c.isEmpty())
      .distinct()
      .collect(Collectors.toList());

    payload.put("country", value( String.join(",", countryList)) );

    PointStruct point = PointStruct.newBuilder()
      .setId(id(UUID.nameUUIDFromBytes(id.getBytes())))
      .setVectors(vectors(vector))
      .putAllPayload(payload)
      .build();

    qdrantClient.upsertAsync(UpsertPoints.newBuilder()
      .setCollectionName(COLLECTION)
      .addPoints(point)
      .build()).get();
  }

  // Helper to extract string arrays from JsonNodes
  private String[] getArrayFromJson(JsonNode node, String field) {
    List<String> list = new ArrayList<>();
    node.forEach(n -> list.add(field == null ? n.asText() : n.path(field).asText()));
    return list.toArray(new String[0]);
  }

  private String[] getArrayFromJson(JsonNode node) {
    return getArrayFromJson(node, null);
  }
  private String convertInvertedIndex(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) return "";
    TreeMap<Integer, String> map = new TreeMap<>();
    node.fields().forEachRemaining(entry -> {
      String word = entry.getKey();
      for (JsonNode pos : entry.getValue()) {
        map.put(pos.asInt(), word);
      }
    });
    return String.join(" ", map.values());
  }
}