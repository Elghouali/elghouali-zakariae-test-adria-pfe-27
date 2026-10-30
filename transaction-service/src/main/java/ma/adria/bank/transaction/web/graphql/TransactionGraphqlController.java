package ma.adria.bank.transaction.web.graphql;
import java.math.BigDecimal;
import java.util.*;
import ma.adria.bank.transaction.domain.*;
import ma.adria.bank.transaction.service.TransactionService;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
@Controller
public class TransactionGraphqlController {
    public record Input(String requestId,String accountId,BigDecimal amount,String currency,String merchantCategory,String country,String targetAccountId) {}
    private final TransactionService service;
    public TransactionGraphqlController(TransactionService service) { this.service=service; }
    @QueryMapping public List<Transaction> transactions() { return service.list(); }
    @QueryMapping public Transaction transaction(@Argument String id) { return service.get(UUID.fromString(id)); }
    @MutationMapping public Transaction createTransaction(@Argument Input input) {
        return service.create(new TransactionRequest(UUID.fromString(input.requestId()),UUID.fromString(input.accountId()),input.amount(),input.currency(),input.merchantCategory(),input.country(),input.targetAccountId()==null?null:UUID.fromString(input.targetAccountId())));
    }
}
