package org.example.integration.mongo;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bson.Document;
import org.bson.conversions.Bson;

public final class MongoClient implements AutoCloseable {

  private final com.mongodb.client.MongoClient client;
  private final String databaseName;

  MongoClient(String connectionString, String databaseName) {
    this.client = MongoClients.create(Objects.requireNonNull(connectionString));
    this.databaseName = Objects.requireNonNull(databaseName, "databaseName cannot be null");
  }

  public MongoDatabase database() {
    return client.getDatabase(databaseName);
  }

  public MongoCollection<Document> collection(String collectionName) {
    return database().getCollection(collectionName);
  }

  public long countDocuments(String collectionName) {
    return collection(collectionName).countDocuments();
  }

  public long countDocuments(String collectionName, Bson filter) {
    return collection(collectionName).countDocuments(filter);
  }

  public List<Document> findAll(String collectionName) {
    return collection(collectionName).find().into(new ArrayList<>());
  }

  public void insertOne(String collectionName, Map<String, Object> document) {
    collection(collectionName).insertOne(new Document(document));
  }

  public void cleanApplicationCollections() {
    collection("player_profiles").deleteMany(new Document());
    collection("processed_events").deleteMany(new Document());
  }

  @Override
  public void close() {
    client.close();
  }
}
