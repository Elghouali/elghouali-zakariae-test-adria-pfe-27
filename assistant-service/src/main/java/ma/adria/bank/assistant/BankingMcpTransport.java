
package ma.adria.bank.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class BankingMcpTransport {

    public enum Service {
        ACCOUNT, TRANSACTION
    }

    private final BankingAuthorization authorization;
    private final HttpClient httpClient;
    private final URI accountUri;
    private final URI transactionUri;

    public BankingMcpTransport(
            BankingAuthorization authorization,
            @Value("${bank.mcp.account-url:http://localhost:8081/mcp}")
            URI accountUri,
            @Value("${bank.mcp.transaction-url:http://localhost:8082/mcp}")
            URI transactionUri) {

        this.authorization = authorization;
        this.accountUri = accountUri;
        this.transactionUri = transactionUri;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String send(
            Service service,
            String jsonRpcPayload,
            JwtAuthenticationToken caller) throws IOException {

        if (jsonRpcPayload == null || jsonRpcPayload.isBlank()) {
            throw new IllegalArgumentException("MCP payload required");
        }

        URI destination;

        if (service == Service.ACCOUNT) {
            authorization.requireScope(caller, "accounts:manage");
            destination = accountUri;
        } else if (service == Service.TRANSACTION) {
            authorization.requireScope(caller, "transactions:manage");
            destination = transactionUri;
        } else {
            throw new IllegalArgumentException("Unknown banking service");
        }

        String token = caller.getToken().getTokenValue();

        HttpRequest request = HttpRequest.newBuilder(destination)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcPayload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "MCP request failed with HTTP status "
                                + response.statusCode()
                );
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("MCP request interrupted", e);
        }
    }
}
