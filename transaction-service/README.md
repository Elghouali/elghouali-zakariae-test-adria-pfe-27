# Transaction Service

Records debits and optional same-currency transfers. All REST, GraphQL and MCP operations call `TransactionService`. The document's transaction fields are retained; `targetAccountId` and processing status make transfers and uncertain outcomes visible.

## Rules

- Positive `BigDecimal` amount, at most Config Server's `bank.transaction.max-amount=1000`.
- Existing ACTIVE accounts, matching currencies, sufficient source funds.
- Consult balances before recording a new operation. Account Service rechecks and applies each debit/transfer atomically after the transaction record is saved.
- Amounts support up to 34 digits and scale from -18 to 18; extreme representations are rejected before arithmetic. Account Service also rejects results that cannot fit its specified double balance model without decimal value loss.
- A client-supplied UUID `requestId` is the transaction ID and retry key. Reusing it with different data is rejected.

## APIs

Default URL: `http://localhost:8082`.

- POST `/api/transactions`, GET `/api/transactions`, GET `/api/transactions/{id}`.
- POST `/graphql`: `transactions`, `transaction(id)`, `createTransaction(input)`.
- POST `/mcp`: `list_transactions`, `get_transaction`, `create_transaction`.

Example REST body (omit targetAccountId for a normal debit):

```json
{
  "requestId": "11111111-1111-4111-8111-111111111111",
  "accountId": "22222222-2222-4222-8222-222222222222",
  "amount": "25.50",
  "currency": "EUR",
  "merchantCategory": "retail",
  "country": "FR"
}
```

For a transfer, add `targetAccountId`. GraphQL's `Decimal` scalar and MCP's amount argument use strings to preserve decimal precision. MCP creation takes the fields above as individual tool arguments.

## Security and dependencies

All interfaces require `transactions:manage`. The same Keycloak JWT is relayed to Account Service and must also include `accounts:manage`. Issuer/audience variables match Account Service. MCP captures the validated authentication per request and restores/clears the worker context after invocation.

OpenFeign discovers `account-service` through Eureka, or uses `ACCOUNT_SERVICE_URL`. Resilience4j supplies a circuit breaker and bounded timeout; its fallback throws an error and never invents accounts or balances. Mutation calls are not automatically retried.

Start Config Server, Eureka, Account Service, Keycloak and Kafka before using real transaction creation. `KAFKA_BOOTSTRAP_SERVERS` defaults to `localhost:9092`.

## Processing and events

`PENDING` means a transaction is recorded but its account movement is not confirmed. `APPLIED` means balances changed but Kafka publication is not confirmed. `COMPLETED` means both are confirmed. A definitive movement rejection is `REJECTED`.

Uncertain processing returns HTTP 503 with the transaction ID. Inspect it and retry with exactly the same requestId/body. Account Service deduplicates successful movements, so a lost response does not debit twice. Kafka retries reuse the same eventId; duplicate delivery is possible.

After persistence and confirmed balance movement, publish the required `TransactionCreated` event to `bank.transactions.created`, keyed by the source accountId. Kafka publication failures remain visible; they are not silently treated as success.

## Limits

Storage and operation deduplication are in-memory and reset on restart. This checkpoint does not provide durable database persistence, distributed transactions or automatic recovery after restart. Live Keycloak/Kafka delivery verification remains pending; test sources cover policy, interfaces, token forwarding, publisher arguments/failures and retry behavior. The latest checkpoint was compiled and packaged with tests skipped at the user's request; its tests have not been confirmed passing. No additional database or messaging technology is introduced.
