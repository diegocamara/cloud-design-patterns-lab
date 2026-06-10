# 03 - Bulkhead

Bulkhead is a pattern used to limit concurrent access to a resource, preventing failures or excessive load in one part of the system from affecting other parts.

Scenario examples can be found in the [`test classes`](./src/test/java/com/example), covering concurrent call limits, immediate rejection, permission waiting, and resource isolation.

## Test scenarios

The scenarios use HTTP integration tests with the application running on a random port. Multiple requests are released concurrently using a thread pool and a `CountDownLatch`, while each test defines different Bulkhead limits and wait durations. The results and gateway counters verify which calls were processed or rejected by the fallback.

## Dependencies

- Spring Boot 4.0.6 for application and web infrastructure;
- Resilience4j 2.4.0 for the Bulkhead implementation and Spring integration;
- AspectJ for applying Resilience4j annotations;
- Spring Boot Test and TestRestTemplate for HTTP integration tests;
- Lombok for annotation processing support.

## How to run

In this subproject folder, run:

```bash
mvn test
```
