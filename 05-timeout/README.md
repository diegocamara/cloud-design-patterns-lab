# 05 - Timeout

Timeout is a pattern used to limit how long an application waits for an external dependency before failing the operation. It helps prevent slow services from keeping callers blocked indefinitely.

Scenario examples can be found in [`TimeoutPatternCasesTests`](./src/test/java/com/example/TimeoutPatternCasesTests.java).

## Test scenarios

- without the timeout pattern, the application waits for the payment service response;
- when the payment service responds before the timeout, the payment status is returned successfully;
- when the payment service does not respond before the timeout, the operation fails fast with `PaymentGatewayTimeoutException`;
- payment service errors are not converted to timeout exceptions.

## How to run

In this subproject folder, run:

```bash
mvn test
```
