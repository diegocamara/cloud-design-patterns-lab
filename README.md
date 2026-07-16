# Cloud Design Patterns Lab

This repository is a study project about cloud design patterns.

The idea is to add subprojects gradually as each pattern is studied. Each folder works as an independent lab, with code examples and tests demonstrating the pattern behavior in scenarios close to real integrations.

## Subprojects

| Project | Pattern |
| --- | --- |
| [01-retry](./01-retry) | Retry |
| [02-circuit-breaker](./02-circuit-breaker) | Circuit Breaker |
| [03-bulkhead](./03-bulkhead) | Bulkhead |
| [04-queue-based-load-leveling](./04-queue-based-load-leveling) | Queue-Based Load Leveling |
| [05-timeout](./05-timeout) | Timeout |
| [06-cqrs](./06-cqrs) | CQRS |
| [07-cache-aside](./07-cache-aside) | Cache-Aside |

## Goal

The goal of this lab is to practice patterns used in distributed systems and cloud-native applications, understanding:

- which problem each pattern solves;
- when to apply the pattern;
- which trade-offs it introduces;
- how to test the expected behavior;
- how libraries like Resilience4j help with the implementation.

## How to use

Enter a subproject and run the tests with Maven. Example:

```bash
cd 01-retry
mvn test
```

New patterns will be added as the study progresses.
