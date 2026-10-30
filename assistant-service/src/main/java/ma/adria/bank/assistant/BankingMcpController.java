
package ma.adria.bank.assistant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mcp")
public class BankingMcpController {

    private final BankingMcpClientService mcpService;
    private final BankingAuthorization authorization;

    public BankingMcpController(
            BankingMcpClientService mcpService,
            BankingAuthorization authorization) {

        this.mcpService = mcpService;
        this.authorization = authorization;
    }

    public record ToolDiscoveryResponse(
            String service,
            int count,
            List<String> tools
    ) {}

    @GetMapping("/tools/{service}")
    public ToolDiscoveryResponse discoverTools(
            @PathVariable String service,
            Authentication authentication) {

        JwtAuthenticationToken caller =
                authorization.requireBankingAccess(authentication);

        BankingMcpTransport.Service target =
                switch (service.toLowerCase(java.util.Locale.ROOT)) {
                    case "account" ->
                            BankingMcpTransport.Service.ACCOUNT;

                    case "transaction" ->
                            BankingMcpTransport.Service.TRANSACTION;

                    default -> throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unknown banking service");
                };

        // The MCP service enforces its specific JWT scope.
        List<String> tools =
                mcpService.discoverTools(target, caller);

        return new ToolDiscoveryResponse(
                service.toLowerCase(java.util.Locale.ROOT),
                tools.size(),
                tools
        );
    }
}
