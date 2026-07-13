package org.example.integration.kafka;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public final class KafkaClient implements AutoCloseable {

  private final String bootstrapServers;
  private final String topic;
  private final AdminClient adminClient;
  private final KafkaProducer<String, String> producer;

  KafkaClient(String bootstrapServers, String topic) {
    this.bootstrapServers = Objects.requireNonNull(bootstrapServers);
    this.topic = Objects.requireNonNull(topic);
    this.adminClient = AdminClient.create(adminProperties());
    this.producer = new KafkaProducer<>(producerProperties());
  }

  public String topic() {
    return topic;
  }

  public RecordMetadata send(String key, String payload) {
    return send(key, payload, Map.of());
  }

  public RecordMetadata send(String key, String payload, Map<String, String> headers) {
    var record = new ProducerRecord<>(topic, key, payload);
    headers.forEach(
        (name, value) -> record.headers().add(name, value.getBytes(StandardCharsets.UTF_8)));
    return send(record);
  }

  public RecordMetadata send(ProducerRecord<String, String> record) {
    try {
      return producer.send(record).get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka send was interrupted", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException("Could not send Kafka record", exception);
    }
  }

  public List<ConsumerRecord<String, String>> consumeFromBeginning(Duration timeout) {
    return consumeFromBeginning("integration-tests-" + UUID.randomUUID(), timeout);
  }

  public List<ConsumerRecord<String, String>> consumeFromBeginning(
      String groupId, Duration timeout) {
    try (var consumer = new KafkaConsumer<String, String>(consumerProperties(groupId))) {
      var partitions = partitions();
      consumer.assign(partitions);
      consumer.seekToBeginning(partitions);
      return pollUntilTimeout(consumer, timeout);
    }
  }

  public void cleanApplicationTopics() {
    var partitions = partitions();
    if (partitions.isEmpty()) {
      return;
    }

    try (var consumer =
        new KafkaConsumer<String, String>(
            consumerProperties("integration-tests-cleaner-" + UUID.randomUUID()))) {
      consumer.assign(partitions);
      consumer.seekToEnd(partitions);

      var recordsToDelete = new HashMap<TopicPartition, RecordsToDelete>();
      for (TopicPartition partition : partitions) {
        recordsToDelete.put(partition, RecordsToDelete.beforeOffset(consumer.position(partition)));
      }

      adminClient.deleteRecords(recordsToDelete).all().get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka cleanup was interrupted", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException("Could not clean Kafka topic " + topic, exception);
    }
  }

  @Override
  public void close() {
    producer.close();
    adminClient.close();
  }

  private List<ConsumerRecord<String, String>> pollUntilTimeout(
      KafkaConsumer<String, String> consumer, Duration timeout) {
    var records = new ArrayList<ConsumerRecord<String, String>>();
    var deadline = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadline) {
      consumer.poll(Duration.ofMillis(100)).forEach(records::add);
    }

    return records;
  }

  private List<TopicPartition> partitions() {
    try (var consumer =
        new KafkaConsumer<String, String>(
            consumerProperties("integration-tests-partitions-" + UUID.randomUUID()))) {
      return consumer.partitionsFor(topic).stream()
          .map(partition -> new TopicPartition(topic, partition.partition()))
          .toList();
    }
  }

  private Properties adminProperties() {
    var properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return properties;
  }

  private Properties producerProperties() {
    var properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    return properties;
  }

  private Properties consumerProperties(String groupId) {
    var properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    return properties;
  }
}
