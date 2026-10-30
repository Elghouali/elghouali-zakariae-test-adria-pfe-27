package ma.adria.bank.account;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.name=account-test", "spring.ai.mcp.server.protocol=STATELESS", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class AccountApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JwtDecoder jwtDecoder;

    private RequestPostProcessor manager() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_accounts:manage"));
    }

    private String createAccount() throws Exception {
        var result = mvc.perform(post("/api/accounts").with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerReference\":\"customer-test\",\"currency\":\"EUR\",\"balance\":100}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(header().exists("Location")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void allTransportsRequireAuthenticationAndTheManagementScope() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"{ accounts { id } }\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/mcp").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restCreatesReadsAndUpdatesThroughTheServiceLayer() throws Exception {
        var id = createAccount();
        mvc.perform(get("/api/accounts/{id}", id).with(manager()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(100));
        mvc.perform(get("/api/accounts").with(manager())).andExpect(status().isOk());
        mvc.perform(put("/api/accounts/{id}/balance", id).with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedBalance\":100,\"balance\":90}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(90));
        mvc.perform(put("/api/accounts/{id}/balance", id).with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedBalance\":100,\"balance\":80}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/accounts/{id}/status", id).with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(put("/api/accounts/{id}/balance", id).with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedBalance\":90,\"balance\":80}"))
                .andExpect(status().isConflict());
    }

    @Test
    void restRejectsInvalidInputAndMissingAccounts() throws Exception {
        mvc.perform(post("/api/accounts").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerReference\":\"\",\"currency\":\"EUR\",\"balance\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").with(manager()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/accounts/{id}", UUID.randomUUID()).with(manager())).andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/not-a-uuid").with(manager())).andExpect(status().isBadRequest());
    }

    private JsonNode graph(String query) throws Exception {
        var result = mvc.perform(post("/graphql").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("query", query))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void graphqlAndRestShareTheSameAccountStateAndValidation() throws Exception {
        var created = graph("mutation { createAccount(input:{customerReference:\"gql-customer\", currency:\"MAD\", balance:50}) { id status } }");
        assertThat(created.has("errors")).isFalse();
        var id = created.at("/data/createAccount/id").asText();
        assertThat(graph("{ account(id:\"" + id + "\") { currency balance } }").at("/data/account/currency").asText()).isEqualTo("MAD");
        assertThat(graph("{ accounts { id } }").has("errors")).isFalse();
        assertThat(graph("mutation { updateAccountBalance(id:\"" + id + "\", expectedBalance:50, balance:40) { balance } }")
                .at("/data/updateAccountBalance/balance").asDouble()).isEqualTo(40);
        assertThat(graph("mutation { changeAccountStatus(id:\"" + id + "\", status:BLOCKED) { status } }")
                .at("/data/changeAccountStatus/status").asText()).isEqualTo("BLOCKED");
        mvc.perform(get("/api/accounts/{id}", id).with(manager())).andExpect(jsonPath("$.balance").value(40));
        assertThat(graph("mutation { createAccount(input:{customerReference:\"\",currency:\"EUR\",balance:0}) { id } }")
                .has("errors")).isTrue();
        assertThat(graph("{ account(id:\"" + UUID.randomUUID() + "\") { id } }").at("/errors/0/extensions/classification").asText()).isEqualTo("NOT_FOUND");
    }

    private JsonNode mcp(String method, Object parameters) throws Exception {
        var payload = java.util.Map.of("jsonrpc", "2.0", "id", 1, "method", method, "params", parameters);
        MvcResult result = mvc.perform(post("/mcp").with(manager())
                .contentType(MediaType.APPLICATION_JSON).accept("application/json", "text/event-stream")
                .content(json.writeValueAsString(payload))).andExpect(status().isOk()).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn();
        }
        var body = result.getResponse().getContentAsString();
        if (body.startsWith("event:") || body.startsWith("data:")) {
            body = body.lines().filter(line -> line.startsWith("data:")).map(line -> line.substring(5).trim()).findFirst().orElseThrow();
        }
        return json.readTree(body);
    }

    @Test
    void mcpListsAndInvokesAccountToolsAgainstTheSameService() throws Exception {
        var tools = mcp("tools/list", java.util.Map.of());
        assertThat(tools.toString()).contains("list_accounts", "get_account", "create_account", "change_account_status", "update_account_balance");
        var created = mcp("tools/call", java.util.Map.of("name", "create_account", "arguments",
                java.util.Map.of("customerReference", "mcp-customer", "currency", "EUR", "balance", 25)));
        assertThat(created.at("/result/isError").asBoolean()).isFalse();
        var text = created.at("/result/content/0/text").asText();
        var id = json.readTree(text).get("id").asText();
        var fetched = mcp("tools/call", java.util.Map.of("name", "get_account", "arguments", java.util.Map.of("accountId", id)));
        assertThat(fetched.toString()).contains("mcp-customer");
        mvc.perform(get("/api/accounts/{id}", id).with(manager())).andExpect(jsonPath("$.balance").value(25));
        assertThat(mcp("tools/call", java.util.Map.of("name", "list_accounts", "arguments", java.util.Map.of())).toString()).contains(id);
        assertThat(mcp("tools/call", java.util.Map.of("name", "update_account_balance", "arguments",
                java.util.Map.of("accountId", id, "expectedBalance", 25, "balance", 20))).toString()).contains("20");
        assertThat(mcp("tools/call", java.util.Map.of("name", "change_account_status", "arguments",
                java.util.Map.of("accountId", id, "status", "BLOCKED"))).toString()).contains("BLOCKED");
        var rejected = mcp("tools/call", java.util.Map.of("name", "create_account", "arguments",
                java.util.Map.of("customerReference", "", "currency", "EUR", "balance", 0)));
        assertThat(rejected.at("/result/isError").asBoolean()).isTrue();
    }
}
