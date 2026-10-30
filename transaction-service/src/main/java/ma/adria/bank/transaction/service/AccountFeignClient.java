package ma.adria.bank.transaction.service;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="account-service",url="${bank.account-service.url:}",fallbackFactory=AccountClientFallback.class)
public interface AccountFeignClient {
    record MovementInput(UUID operationId,UUID targetAccountId,BigDecimal amount,String currency) {}
    @GetMapping("/api/accounts/{id}")
    Accounts.AccountView get(@PathVariable UUID id,@RequestHeader("Authorization") String authorization);
    @PostMapping("/api/accounts/{id}/movements")
    void apply(@PathVariable UUID id,@RequestBody MovementInput input,@RequestHeader("Authorization") String authorization);
}
