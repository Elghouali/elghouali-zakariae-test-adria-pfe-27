# ADRIA ENSET 2027 - Banking platform

Initial project structure for the agreed test architecture. **This is a scaffold, not a running banking application.**

## Modules

- `account-service`
- `transaction-service`
- `gateway-service`
- `discovery-service`
- `config-service`
- `assistant-service`
- `transaction-consumer`

- `frontend/`: Angular application structure.
- `fraud-detection/`: separate Python experiment.
- `docs/`: architecture, screenshots and eventual PDF report.

## Required technologies

Spring Cloud Gateway, Eureka, Config Server, OpenFeign, Resilience4j, Spring AI, REST, GraphQL, MCP, Kafka, Keycloak, Angular, Python, Maven, Docker, Docker Compose and GitHub Actions.

No database or LLM provider has been selected. Spring and Angular dependencies will be introduced during implementation. Java 21 is used by scaffold validation.

## Validate structure

```shell
mvn validate
```

The Maven reactor contains seven modules. There is no application code or test suite yet. Dockerfiles and Compose are explicitly marked placeholders; containers cannot run yet. The GitHub workflow validates Maven structure only; remaining requested CI/CD stages are pending implementation.

See [architecture](docs/architecture/architecture.md).

## Delivery

The test requests a private repository initially, commits at 15-minute intervals during the exercise, a PDF report with test screenshots, and public visibility at the end. Visibility changes and classroom/Google Sheet submissions are separate steps; the scaffold does not automate them.

