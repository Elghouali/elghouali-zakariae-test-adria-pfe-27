
package ma.adria.bank.assistant;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.time.Duration;

import org.springframework.stereotype.Service;

@Service
public class BankingMcpClientService {

    public McpSyncClient createClient(String baseUrl) {

        McpClientTransport transport =
                HttpClientStreamableHttpTransport
                        .builder(baseUrl)
                        .endpoint("/mcp")
                        .build();

        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
    }
}
