# 02 - Circuit Breaker

Circuit Breaker is a pattern used to avoid repeated calls to a service that is failing or too slow, temporarily opening the circuit until the service has a chance to recover.

Scenario examples can be found in the test classes, especially in [`CircuitBreakerPatternCasesTests`](./src/test/java/com/example/CircuitBreakerPatternCasesTests.java).

## How to run

In this subproject folder, run:

```bash
mvn test
```
