
package ma.adria.bank.assistant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class BankingAuthorization {

    public JwtAuthenticationToken requireBankingAccess(
            Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !jwt.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Valid JWT authentication required");
        }

        boolean accounts = jwt.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("SCOPE_accounts:manage"));

        boolean transactions = jwt.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("SCOPE_transactions:manage"));

        if (!accounts && !transactions) {
            throw new AccessDeniedException(
                    "Missing banking permissions");
        }

        return jwt;
    }

    public void requireScope(
            JwtAuthenticationToken jwt, String scope) {

        if (!jwt.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("SCOPE_" + scope))) {
            throw new AccessDeniedException(
                    "Missing required scope: " + scope);
        }
    }
}
