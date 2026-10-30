
package ma.adria.bank.assistant;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class BankingMcpClientService {

    private final BankingAuthorization authorization;
    private final String accountUrl;
    private final String transactionUrl;

    public BankingMcpClientService(
            BankingAuthorization authorization,
            @Value("${bank.mcp.account-url}")
            String accountUrl,
            @Value("${bank.mcp.transaction-url}")
            String transactionUrl) {

        this.authorization = authorization;
        this.accountUrl = accountUrl;
        this.transactionUrl = transactionUrl;
    }

    public List<String> discoverTools(
            BankingMcpTransport.Service service,
            JwtAuthenticationToken caller) {

        if (caller == null || !caller.isAuthenticated()) {
            throw new org.springframework.security.access
                    .AccessDeniedException("Authentication required");
        }

        String scope;
        String baseUrl;

        if (service == BankingMcpTransport.Service.ACCOUNT) {
            scope = "accounts:manage";
            baseUrl = accountUrl;
        } else if (service == BankingMcpTransport.Service.TRANSACTION) {
            scope = "transactions:manage";
            baseUrl = transactionUrl;
        } else {
            throw new IllegalArgumentException("Unknown MCP service");
        }

        authorization.requireScope(caller, scope);

        // Token belongs only to this authenticated operation.
        String token = caller.getToken().getTokenValue();

        // Configuration contains the complete /mcp URL.
        String serverUrl = baseUrl.substring(
                0, baseUrl.length() - "/mcp".length());

        McpClientTransport transport =
                HttpClientStreamableHttpTransport
                        .builder(serverUrl)
                        .endpoint("/mcp")
                        .httpRequestCustomizer(
                                (builder, method, endpoint, body, context) ->
                                        builder.header(
                                                "Authorization",
                                                "Bearer " + token))
                        .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        try {
            client.initialize();

            return client.listTools()
                    .tools()
                    .stream()
                    .map(tool -> tool.name())
                    .toList();

        } finally {
            client.closeGracefully();
        }
    }
}
