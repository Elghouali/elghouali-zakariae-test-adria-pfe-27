# Account Service

Implements the document's account fields: UUID `id`, `customerReference`, `ACTIVE | BLOCKED` status, `currency` and `double balance`. REST, GraphQL and MCP call the same `AccountService`.

## Run

Start Config Server and Eureka first, then:

```shell
mvn -pl account-service -am verify
java -jar account-service/target/account-service-0.0.1-SNAPSHOT.jar
```

Default address: `http://localhost:8081`. Configuration uses the same `CONFIG_SERVER_URL`, `EUREKA_URL`, `SERVER_ADDRESS` and `SERVER_PORT` overrides as the infrastructure services.

## Authentication and authorization

All three interfaces require a Keycloak bearer token containing the `accounts:manage` scope. This is an administrative permission to manage all test accounts, not customer self-service access.

Set `KEYCLOAK_ISSUER_URI` (default `http://localhost:8180/realms/bank`) and `KEYCLOAK_AUDIENCE` (default `bank-api`). Configure the corresponding client scope and audience in Keycloak. No token or client secret is stored in the source. Live Keycloak realm setup and token validation remain to be tested; automated interface tests mock the JWT decoder and verify the authentication/permission boundary.

## REST

| Method | Path | Body |
|---|---|---|
| POST | `/api/accounts` | `{"customerReference":"customer-1","currency":"EUR","balance":100}` |
| GET | `/api/accounts` | None |
| GET | `/api/accounts/{id}` | None |
| PATCH | `/api/accounts/{id}/status` | `{"status":"BLOCKED"}` |
| PUT | `/api/accounts/{id}/balance` | `{"expectedBalance":100,"balance":90}` |

New accounts start ACTIVE. Invalid input returns 400, unknown accounts 404, and blocked/stale balance updates 409. Balance replacement checks account status and the previously consulted balance atomically. It is not a transfer or an idempotent transaction protocol; Transaction Service will coordinate banking transactions.

## GraphQL and MCP

POST GraphQL queries to `/graphql`. The schema is in `src/main/resources/graphql/account.graphqls` and exposes `accounts`, `account`, `createAccount`, `changeAccountStatus` and `updateAccountBalance`.

Example query:

```graphql
query { accounts { id customerReference status currency balance } }
```

The stateless HTTP MCP endpoint is `/mcp`. Tools: `list_accounts`, `get_account`, `create_account`, `change_account_status`, `update_account_balance`. Bearer authentication applies to MCP requests too.

## Current limits

- Storage is an in-memory Java repository: restart loses all accounts. No database technology was specified or introduced.
- Balance remains `double` as requested by the document. Only finite values are accepted; no overdraft policy is assumed.
- Expected-balance comparison prevents a stale replacement when the current balance differs. It does not provide operation deduplication or distributed transaction guarantees.
- Transaction Service now orchestrates debits/transfers. POST `/api/accounts/{id}/movements` accepts operationId, optional targetAccountId, amount and currency; successful operation IDs are deduplicated while this instance is running. Debit/transfer checks and both transfer balance changes share one repository lock.

Tests cover service validation, concurrent balance updates, REST/GraphQL/MCP operations and cross-interface state, and unauthenticated/insufficient-permission requests. JaCoCo enforces 80% line coverage for this module.
