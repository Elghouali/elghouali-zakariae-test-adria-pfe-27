package ma.adria.bank.transaction.service;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class FeignAccounts implements Accounts {
    private final AccountFeignClient client;
    public FeignAccounts(AccountFeignClient client) { this.client=client; }
    public AccountView get(UUID id) { return client.get(id,authorization()); }
    public void apply(UUID operationId,UUID source,UUID target,BigDecimal amount,String currency) {
        client.apply(source,new AccountFeignClient.MovementInput(operationId,target,amount,currency),authorization());
    }
    private String authorization() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof JwtAuthenticationToken jwt) return "Bearer "+jwt.getToken().getTokenValue();
        throw new IllegalStateException("An authenticated bearer token is required for Account Service");
    }
}
