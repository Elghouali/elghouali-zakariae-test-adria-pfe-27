package ma.adria.bank.account.web.graphql;

import java.util.List;
import java.util.UUID;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.service.AccountService;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

@Controller
public class AccountGraphqlController {
    private final AccountService service;
    public AccountGraphqlController(AccountService service) { this.service = service; }
    public record CreateAccountInput(String customerReference, String currency, Double balance) {}

    @QueryMapping
    public List<Account> accounts() { return service.list(); }

    @QueryMapping
    public Account account(@Argument String id) { return service.get(UUID.fromString(id)); }

    @MutationMapping
    public Account createAccount(@Argument CreateAccountInput input) {
        return service.create(input.customerReference(), input.currency(), input.balance());
    }

    @MutationMapping
    public Account changeAccountStatus(@Argument String id, @Argument AccountStatus status) {
        return service.changeStatus(UUID.fromString(id), status);
    }

    @MutationMapping
    public Account updateAccountBalance(@Argument String id, @Argument Double expectedBalance, @Argument Double balance) {
        return service.updateBalance(UUID.fromString(id), expectedBalance, balance);
    }
}
