
package ma.adria.bank.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class BankingChatController {

    private final BankingChatService service;
    private final BankingAuthorization authorization;

    public BankingChatController(
            BankingChatService service,
            BankingAuthorization authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    public record ChatRequest(
            @NotBlank
            @Size(max = 4000)
            String message) {}

    public record ChatResponse(String reply) {}

    @PostMapping
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        authorization.requireBankingAccess(authentication);

        return new ChatResponse(
                service.chat(request.message()));
    }
}
