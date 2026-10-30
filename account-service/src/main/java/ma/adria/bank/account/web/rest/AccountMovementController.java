package ma.adria.bank.account.web.rest;

import java.math.BigDecimal;
import java.util.UUID;
import ma.adria.bank.account.service.AccountMovementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/{id}")
public class AccountMovementController {
    public record MovementInput(UUID operationId, UUID targetAccountId, BigDecimal amount, String currency) {}
    private final AccountMovementService service;
    public AccountMovementController(AccountMovementService service) { this.service=service; }
    @PostMapping("/movements")
    public AccountMovementService.MovementResult apply(@PathVariable UUID id,@RequestBody MovementInput input) {
        return service.apply(input.operationId(),id,input.targetAccountId(),input.amount(),input.currency());
    }
}
