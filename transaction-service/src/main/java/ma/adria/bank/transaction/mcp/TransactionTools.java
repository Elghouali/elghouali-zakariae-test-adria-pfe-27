package ma.adria.bank.transaction.mcp;
import java.math.BigDecimal;
import java.util.*;
import ma.adria.bank.transaction.domain.*;
import ma.adria.bank.transaction.service.TransactionService;
import org.springframework.ai.mcp.annotation.*;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
@Component
public class TransactionTools {
    private final TransactionService service;
    public TransactionTools(TransactionService service) { this.service=service; }
    @McpTool(name="list_transactions",description="List transactions and their processing status")
    public List<Transaction> list() { return service.list(); }
    @McpTool(name="get_transaction",description="Consult a transaction by its requestId")
    public Transaction get(@McpToolParam(required=true) String transactionId) { return service.get(UUID.fromString(transactionId)); }
    @McpTool(name="create_transaction",description="Record a debit or same-currency transfer; amount is an exact decimal string; reuse requestId on retries")
    public Transaction create(@McpToolParam(required=true) String requestId,@McpToolParam(required=true) String accountId,
            @McpToolParam(required=true) String amount,@McpToolParam(required=true) String currency,
            @McpToolParam(required=true) String merchantCategory,@McpToolParam(required=true) String country,
            @McpToolParam(required=false) String targetAccountId,McpSyncRequestContext context) {
        var previous=SecurityContextHolder.getContext();
        var scoped=SecurityContextHolder.createEmptyContext();
        scoped.setAuthentication((Authentication)context.transportContext().get("authentication"));
        try {
            SecurityContextHolder.setContext(scoped);
            return service.create(new TransactionRequest(UUID.fromString(requestId),UUID.fromString(accountId),new BigDecimal(amount),currency,merchantCategory,country,targetAccountId==null?null:UUID.fromString(targetAccountId)));
        } finally { SecurityContextHolder.setContext(previous); }
    }
}
