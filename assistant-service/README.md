
# ADRIA Banking AI Assistant

## Overview

Spring AI banking assistant using JWT authentication
and MCP integration with Account and Transaction Services.

## Components

- AssistantApplication: Spring Boot entry point
- AssistantSecurity: Keycloak JWT authentication
- BankingAuthorization: banking scope authorization
- BankingChatService: Spring AI conversation
- BankingChatController: authenticated chat API
- BankingMcpTransport: low-level JWT HTTP bridge
- BankingMcpClientService: MCP SDK client and discovery
- BankingMcpController: authenticated tool discovery
- BankingMcpExceptionHandler: upstream error handling

## API

POST /api/chat

Request:
{"message":"Explain how bank transfers work"}

GET /api/mcp/tools/account

Requires JWT scope: accounts:manage

GET /api/mcp/tools/transaction

Requires JWT scope: transactions:manage

## Security

- Keycloak JWT resource server
- JWT audience validation configured
- Separate account and transaction scopes
- No hard-coded banking credentials
- Per-operation MCP client construction
- Automatic AI tool calling disabled

## Limitations

- Keycloak runtime integration unverified
- MCP connection and tool discovery unverified
- AI-driven banking operations disabled
- Transaction and account storage remains in-memory
- Data is lost when the owning service restarts
- No durable database persistence

## Development

Java 21
Spring Boot 4
Spring AI 2
MCP Java SDK 2.1.0

Compile using IntelliJ Maven lifecycle.

Tests are intentionally not executed during
the current development checkpoint.
