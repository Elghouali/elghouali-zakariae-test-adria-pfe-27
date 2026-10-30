package ma.adria.bank.account.web.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    public record CreateAccount(String customerReference, String currency, Double balance) {}
    public record ChangeStatus(AccountStatus status) {}
    public record UpdateBalance(Double expectedBalance, Double balance) {}

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody CreateAccount input) {
        var account = service.create(input.customerReference(), input.currency(), input.balance());
        return ResponseEntity.created(URI.create("/api/accounts/" + account.id())).body(account);
    }

    @GetMapping
    public List<Account> list() { return service.list(); }

    @GetMapping("/{id}")
    public Account get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}/status")
    public Account changeStatus(@PathVariable UUID id, @RequestBody ChangeStatus input) {
        return service.changeStatus(id, input.status());
    }

    @PutMapping("/{id}/balance")
    public Account updateBalance(@PathVariable UUID id, @RequestBody UpdateBalance input) {
        return service.updateBalance(id, input.expectedBalance(), input.balance());
    }
}
