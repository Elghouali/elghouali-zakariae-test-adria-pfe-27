# ADRIA ENSET 2027 - Banking platform

Maven multi-module banking project with an Angular frontend, following the test specification.

## Current checkpoint

- Discovery Service: runnable Eureka server.
- Config Service: runnable native Config Server sharing `bank.transaction.max-amount=1000`.
- Gateway Service: runnable gateway, Config Server client and Eureka client; routes prepared for banking services.
- Angular: CLI-generated standalone application with routing and a minimal starter screen.
- Account Service, Transaction Service, Assistant Service and Transaction Consumer: structure only, implementation pending.
- Python fraud detection, Dockerfiles, Compose and PDF report: pending.

No database or LLM provider has been selected. Banking REST, GraphQL and MCP adapters will share each service's application layer.

## Requirements

Java 21, Maven 3.9+, Node.js 24 and npm. Angular 21 requires Node 24.0 or newer. Backend versions: Spring Boot 4.0.9 and Spring Cloud 2025.1.4.

## Build

```shell
mvn verify
cd frontend
npm ci
npm test -- --watch=false
npm run build
```

The four unimplemented Java modules contain no application code or tests yet. The generated Angular smoke tests verify app creation and rendering. Infrastructure endpoints are also checked manually; there are no banking tests at this checkpoint.

## Run infrastructure locally

After `mvn package`, launch these in separate terminals in this order:

```shell
java -jar discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar
java -jar config-service/target/config-service-0.0.1-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

- Eureka dashboard: http://localhost:8761
- Shared transaction configuration: http://localhost:8888/transaction-service/default
- Gateway: http://localhost:8088
- Angular: run `npm start` in `frontend/`, then open http://localhost:4200.

Infrastructure binds to loopback by default. Gateway requires Config Server at startup. Override `CONFIG_SERVER_URL`, `EUREKA_URL`, `SERVER_PORT` and `SERVER_ADDRESS` when needed.

Gateway routes `/api/accounts/**`, `/api/transactions/**` and `/api/chat/**` are reserved for downstream services; they cannot complete banking requests until those services exist. Keycloak integration is pending, so these services currently expose local development infrastructure only.

Dockerfiles and Compose remain explicit placeholders. GitHub Actions builds the Maven reactor and builds/tests Angular; image publication and the schematic deployment stage remain pending.

See [architecture](docs/architecture/architecture.md), [report notes](docs/README.md) and [fraud experiment](fraud-detection/README.md).

## Delivery

The test requests an initially private repository, commits every 15 minutes during the exercise and a PDF report with test screenshots. Final public visibility, Google Sheet and Classroom submission are separate delivery steps.
