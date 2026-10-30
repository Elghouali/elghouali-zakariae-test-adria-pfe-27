package ma.adria.bank.transaction.web.rest;
import java.net.URI;
import java.util.*;
import ma.adria.bank.transaction.domain.*;
import ma.adria.bank.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service=service; }
    @PostMapping public ResponseEntity<Transaction> create(@RequestBody TransactionRequest input) {
        var transaction=service.create(input);
        return ResponseEntity.created(URI.create("/api/transactions/"+transaction.id())).body(transaction);
    }
    @GetMapping public List<Transaction> list() { return service.list(); }
    @GetMapping("/{id}") public Transaction get(@PathVariable UUID id) { return service.get(id); }
}
