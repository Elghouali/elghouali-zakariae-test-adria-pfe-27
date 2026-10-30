# Architecture

```mermaid
flowchart TB
  Angular --> Gateway[Spring Cloud Gateway]
  Angular --> Keycloak
  Gateway --> Account[Account Service: REST, GraphQL, MCP]
  Gateway --> Transaction[Transaction Service: REST, GraphQL, MCP]
  Gateway --> Assistant[Spring AI Assistant]
  RestClient[REST client] --> Assistant
  Bot[Telegram or Discord client] --> Assistant
  Assistant --> LLM
  Assistant -->|MCP| Account
  Assistant -->|MCP| Transaction
  Transaction -->|OpenFeign + Resilience4j| Account
  Transaction -->|TransactionCreated; key: accountId| Kafka
  Kafka --> Consumer[Event logging consumer]
  Config[Spring Cloud Config] -.->|bank.transaction.max-amount=1000| Transaction
  Account -.-> Eureka
  Transaction -.-> Eureka
  Assistant -.-> Eureka
  Gateway -.-> Eureka
```

Keycloak handles authentication and authorization. Banking REST, GraphQL and MCP adapters share their service layer. Database and LLM providers are unspecified. Python fraud detection remains a separate experiment. Docker Compose runs the application locally; GitHub Actions supplies CI/CD.

The document specifies transaction validation, balance consultation and update, and transfer support. Transaction direction, overdraft rules and transfer failure semantics remain to be clarified.

