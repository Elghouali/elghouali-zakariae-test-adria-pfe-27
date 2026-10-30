package ma.adria.bank.transaction.mcp;
import java.util.Map;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.context.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
@Configuration
public class TransactionMcpContext {
    @Bean WebMvcStatelessServerTransport authenticatedMcpTransport(JsonMapper mapper) {
        return WebMvcStatelessServerTransport.builder().jsonMapper(new JacksonMcpJsonMapper(mapper)).messageEndpoint("/mcp")
                .contextExtractor(request->McpTransportContext.create(Map.of("authentication",SecurityContextHolder.getContext().getAuthentication())))
                .build();
    }
}
