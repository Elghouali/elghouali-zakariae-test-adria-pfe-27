
package ma.adria.bank.assistant;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = BankingMcpController.class)
public class BankingMcpExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleMcpFailure(
            RuntimeException exception) {

        if (exception instanceof AccessDeniedException denied) {
            throw denied;
        }

        if (exception instanceof ResponseStatusException status) {
            throw status;
        }

        // Never expose upstream errors, tokens or internal URLs.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "MCP_UNAVAILABLE",
                        "message",
                        "Banking MCP service is unavailable"
                ));
    }
}
