# 01 - Retry

Retry is a pattern used to try running an operation again when the failure seems temporary, such as a network error, momentary unavailability, or HTTP 5xx responses.

Scenario examples can be found in the test classes, especially in [`RetryPatternCasesTests`](./src/test/java/com/example/RetryPatternCasesTests.java).

## How to run

In this subproject folder, run:

```bash
mvn test
```
