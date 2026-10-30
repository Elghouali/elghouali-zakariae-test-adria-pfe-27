
package ma.adria.bank.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class BankingChatService {

    private final ChatClient chatClient;

    public BankingChatService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                You are the ADRIA banking assistant.
                Answer banking questions clearly.
                You currently have no access to live
                accounts or transactions.
                Never invent balances, transactions,
                or completed banking operations.
                """)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
