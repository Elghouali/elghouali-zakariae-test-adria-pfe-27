
package ma.adria.bank.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class BankingChatController {

    private final BankingChatService service;

    public BankingChatController(BankingChatService service) {
        this.service = service;
    }

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String message
    ) {}

    public record ChatResponse(String reply) {}

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(service.chat(request.message()));
    }
}
