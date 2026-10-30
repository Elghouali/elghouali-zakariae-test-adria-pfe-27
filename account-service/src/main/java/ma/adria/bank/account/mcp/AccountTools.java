package ma.adria.bank.account.mcp;

import java.util.List;
import java.util.UUID;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.service.AccountService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class AccountTools {
    private final AccountService service;
    public AccountTools(AccountService service) { this.service = service; }

    @McpTool(name = "list_accounts", description = "List bank accounts for an authorized account manager")
    public List<Account> listAccounts() { return service.list(); }

    @McpTool(name = "get_account", description = "Consult an account and its current balance")
    public Account getAccount(@McpToolParam(required = true) String accountId) {
        return service.get(UUID.fromString(accountId));
    }

    @McpTool(name = "create_account", description = "Create an active bank account")
    public Account createAccount(@McpToolParam(required = true) String customerReference,
            @McpToolParam(required = true) String currency, @McpToolParam(required = true) Double balance) {
        return service.create(customerReference, currency, balance);
    }

    @McpTool(name = "change_account_status", description = "Set account status to ACTIVE or BLOCKED")
    public Account changeAccountStatus(@McpToolParam(required = true) String accountId,
            @McpToolParam(required = true) String status) {
        return service.changeStatus(UUID.fromString(accountId), AccountStatus.valueOf(status));
    }

    @McpTool(name = "update_account_balance", description = "Update an active account balance only if it still equals the previously consulted balance")
    public Account updateAccountBalance(@McpToolParam(required = true) String accountId,
            @McpToolParam(required = true) Double expectedBalance, @McpToolParam(required = true) Double balance) {
        return service.updateBalance(UUID.fromString(accountId), expectedBalance, balance);
    }
}
