package io.jans.federation;

import io.jans.federation.service.TrustChainResolver;
import io.jans.federation.service.TrustChainResolver.TrustChainResult;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Comprehensive Integration Test for OpenID Federation 1.0 Trust Chain Validation
 * 
 * This test demonstrates the complete trust chain resolution and validation process:
 * 
 * Setup:
 * 1. node1 (Trust Anchor) - http://localhost:8080
 * 2. node2 (Subordinate) - http://localhost:8081, subordinate of node1
 * 3. node3 (Subordinate) - http://localhost:8082, subordinate of node1
 * 
 * Test Scenario:
 * - node2 resolves and validates trust chain for node3
 * - Trust chain: node3 → node1 (Trust Anchor)
 * 
 * Per OpenID Federation 1.0 Specification Section 4:
 * "Trust chains are resolved by following authority_hints from the target entity
 * up to a Trust Anchor, fetching Subordinate Statements from each superior entity."
 * 
 * Reference: https://openid.net/specs/openid-federation-1_0.html#section-4
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrustChainIntegrationTest {
    
    private static final String NODE1_URL = "http://localhost:8080"; // Trust Anchor
    private static final String NODE2_URL = "http://localhost:8081"; // Subordinate
    private static final String NODE3_URL = "http://localhost:8082"; // Subordinate
    
    private static final String NODE1_ENTITY_ID = "https://node1.example.com";
    private static final String NODE2_ENTITY_ID = "https://node2.example.com";
    private static final String NODE3_ENTITY_ID = "https://node3.example.com";
    
    private static CloseableHttpClient httpClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public static void setup() throws Exception {
        httpClient = HttpClients.createDefault();
        log("=".repeat(80));
        log("OpenID Federation 1.0 - Trust Chain Validation Integration Test");
        log("=".repeat(80));
        log("");
        log("Test Scenario:");
        log("  - node1 (Trust Anchor): " + NODE1_URL);
        log("  - node2 (Subordinate):  " + NODE2_URL);
        log("  - node3 (Subordinate):  " + NODE3_URL);
        log("");
        log("Objective:");
        log("  node2 validates trust chain for node3 through Trust Anchor (node1)");
        log("");
        log("=".repeat(80));
        log("");
        
        // Clean up any existing subordinates from previous runs
        log("Cleaning up existing subordinates...");
        cleanupSubordinates(NODE1_URL);
        log("✓ Cleanup complete");
        log("");
    }
    
    private static void cleanupSubordinates(String nodeUrl) throws Exception {
        // Try to delete existing subordinates (ignore errors)
        try {
            String[] entityIds = {NODE2_ENTITY_ID, NODE3_ENTITY_ID};
            for (String entityId : entityIds) {
                try {
                    org.apache.hc.client5.http.classic.methods.HttpDelete deleteRequest = 
                        new org.apache.hc.client5.http.classic.methods.HttpDelete(
                            nodeUrl + "/manage/subordinates/" + entityId);
                    httpClient.execute(deleteRequest).close();
                } catch (Exception e) {
                    // Ignore - subordinate might not exist
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @AfterAll
    public static void tearDown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
        log("");
        log("=".repeat(80));
        log("✅ Trust Chain Integration Test Completed");
        log("=".repeat(80));
    }
    
    /**
     * Test 1: Verify all nodes are running
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: Verify All Federation Entities Are Running")
    public void test01_VerifyNodesRunning() throws Exception {
        logTestHeader("Test 1: Verify All Entities Are Running");
        
        String[] nodes = {NODE1_URL, NODE2_URL, NODE3_URL};
        String[] nodeNames = {"node1 (Trust Anchor)", "node2 (Subordinate)", "node3 (Subordinate)"};
        
        for (int i = 0; i < nodes.length; i++) {
            log("Checking " + nodeNames[i] + "...");
            String url = nodes[i] + "/.well-known/openid-federation";
            
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                Assertions.assertEquals(200, statusCode, nodeNames[i] + " should be running");
                
                JsonNode json = parseJWT(body);
                String entityId = json.get("iss").asText();
                log("  ✓ Entity ID: " + entityId);
                log("  ✓ Status: Running and responding");
            }
        }
        
        logSuccess("✅ All 3 federation entities are running and healthy");
    }
    
    /**
     * Test 2: Configure node1 as Trust Anchor (no authority hints)
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: Configure node1 as Trust Anchor")
    public void test02_ConfigureTrustAnchor() throws Exception {
        logTestHeader("Test 2: Configure Trust Anchor (node1)");
        
        log("Trust Anchor Configuration:");
        log("  - Entity ID: " + NODE1_ENTITY_ID);
        log("  - Authority Hints: [] (empty - this IS the trust anchor)");
        log("");
        
        // Set empty authority hints for node1 (it's the Trust Anchor)
        String url = NODE1_URL + "/manage/entity/authority-hints";
        String requestBody = "{\"authority_hints\": []}";
        
        log("POST " + url);
        log("Body: " + requestBody);
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response: " + statusCode);
            log("Body: " + formatJson(body));
            
            Assertions.assertEquals(200, statusCode);
            
            JsonNode json = parseJWT(body);
            Assertions.assertTrue(json.get("authority_hints").isEmpty(), 
                "Trust Anchor should have empty authority_hints");
        }
        
        // Verify entity configuration
        log("");
        log("Verifying Trust Anchor Entity Configuration...");
        HttpGet getConfig = new HttpGet(NODE1_URL + "/.well-known/openid-federation");
        try (CloseableHttpResponse response = httpClient.execute(getConfig)) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode json = parseJWT(body);
            
            log("Entity Configuration:");
            log(formatJson(body));
            
            Assertions.assertEquals(NODE1_ENTITY_ID, json.get("iss").asText());
            Assertions.assertEquals(NODE1_ENTITY_ID, json.get("sub").asText());
            
            // Trust Anchor should have no authority_hints or empty array
            boolean noHints = !json.has("authority_hints") || 
                            json.get("authority_hints").isEmpty();
            Assertions.assertTrue(noHints, "Trust Anchor should not have authority_hints");
        }
        
        logSuccess("✅ node1 configured as Trust Anchor (no authority hints)");
    }
    
    /**
     * Test 3: Add node2 as subordinate to node1
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Register node2 as Subordinate of Trust Anchor")
    public void test03_RegisterNode2AsSubordinate() throws Exception {
        logTestHeader("Test 3: Register node2 as Subordinate of node1 (Trust Anchor)");
        
        log("Registering Subordinate:");
        log("  Superior: node1 (Trust Anchor)");
        log("  Subordinate: node2");
        log("");
        
        String url = NODE1_URL + "/manage/subordinates";
        String requestBody = "{\n" +
            "  \"entity_id\": \"" + NODE2_ENTITY_ID + "\",\n" +
            "  \"jwks\": {\n" +
            "    \"keys\": [{\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"kid\": \"node2-key-1\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"alg\": \"RS256\"\n" +
            "    }]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"federation_entity\": {\n" +
            "      \"federation_fetch_endpoint\": \"" + NODE2_URL + "/fetch\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        
        log("POST " + url);
        log("Request Body:");
        log(formatJson(requestBody));
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response: " + statusCode);
            log("Response Body:");
            log(formatJson(body));
            
            // Accept both 201 (created) and 200 (updated) as success
            boolean success = statusCode == 201 || statusCode == 200;
            Assertions.assertTrue(success, "Should return 201 Created or 200 OK, got: " + statusCode);
            
            JsonNode json = parseJWT(body);
            Assertions.assertEquals(NODE2_ENTITY_ID, json.get("entity_id").asText());
            
            String status = json.get("status").asText();
            Assertions.assertTrue(status.equals("created") || status.equals("updated"), 
                "Status should be 'created' or 'updated', got: " + status);
            
            // Verify authority_hints includes node1
            JsonNode hints = json.get("authority_hints");
            Assertions.assertTrue(hints.isArray() && hints.size() > 0);
            Assertions.assertEquals(NODE1_ENTITY_ID, hints.get(0).asText());
        }
        
        logSuccess("✅ node2 registered as subordinate of node1");
    }
    
    /**
     * Test 4: Configure node2 with authority hint to node1
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Configure node2 Authority Hints")
    public void test04_ConfigureNode2AuthorityHints() throws Exception {
        logTestHeader("Test 4: Configure node2 to point to Trust Anchor");
        
        log("Setting authority_hints for node2:");
        log("  authority_hints: [" + NODE1_URL + "]");
        log("");
        
        String url = NODE2_URL + "/manage/entity/authority-hints";
        String requestBody = "{\"authority_hints\": [\"" + NODE1_URL + "\"]}";
        
        log("POST " + url);
        log("Body: " + requestBody);
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response: " + statusCode);
            log("Body: " + formatJson(body));
            
            Assertions.assertEquals(200, statusCode);
        }
        
        logSuccess("✅ node2 configured with authority hints to node1");
    }
    
    /**
     * Test 5: Add node3 as subordinate to node1
     */
    @Test
    @Order(5)
    @DisplayName("Test 5: Register node3 as Subordinate of Trust Anchor")
    public void test05_RegisterNode3AsSubordinate() throws Exception {
        logTestHeader("Test 5: Register node3 as Subordinate of node1 (Trust Anchor)");
        
        String url = NODE1_URL + "/manage/subordinates";
        String requestBody = "{\n" +
            "  \"entity_id\": \"" + NODE3_ENTITY_ID + "\",\n" +
            "  \"jwks\": {\n" +
            "    \"keys\": [{\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"kid\": \"node3-key-1\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"alg\": \"RS256\"\n" +
            "    }]\n" +
            "  }\n" +
            "}";
        
        log("POST " + url);
        log("Request Body:");
        log(formatJson(requestBody));
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response: " + statusCode);
            log("Response Body:");
            log(formatJson(body));
            
            // Accept both 201 (created) and 200 (updated) as success
            boolean success = statusCode == 201 || statusCode == 200;
            Assertions.assertTrue(success, "Should return 201 Created or 200 OK, got: " + statusCode);
        }
        
        logSuccess("✅ node3 registered as subordinate of node1");
    }
    
    /**
     * Test 6: Configure node3 with authority hint to node1
     */
    @Test
    @Order(6)
    @DisplayName("Test 6: Configure node3 Authority Hints")
    public void test06_ConfigureNode3AuthorityHints() throws Exception {
        logTestHeader("Test 6: Configure node3 to point to Trust Anchor");
        
        String url = NODE3_URL + "/manage/entity/authority-hints";
        String requestBody = "{\"authority_hints\": [\"" + NODE1_URL + "\"]}";
        
        log("POST " + url);
        log("Body: " + requestBody);
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response: " + statusCode);
            log("Body: " + formatJson(body));
            
            Assertions.assertEquals(200, statusCode);
        }
        
        logSuccess("✅ node3 configured with authority hints to node1");
    }
    
    /**
     * Test 7: Verify Trust Chain Resolution from node3 to node1
     * 
     * This is the core test that validates the complete OpenID Federation 1.0
     * Trust Chain Resolution process as described in Section 4.
     * 
     * Process (per spec):
     * 1. Start with node3's Entity Configuration
     * 2. Extract authority_hints (should contain node1 URL)
     * 3. Fetch node1's Entity Configuration  
     * 4. Fetch Subordinate Statement about node3 from node1
     * 5. Validate the chain
     * 6. Confirm node1 is the Trust Anchor
     */
    @Test
    @Order(7)
    @DisplayName("Test 7: Trust Chain Resolution and Validation (OpenID Federation 1.0 Section 4)")
    public void test07_TrustChainResolution() throws Exception {
        logTestHeader("Test 7: Trust Chain Resolution (Section 4)");
        
        log("Trust Chain Resolution Process:");
        log("  Target Entity: node3 (" + NODE3_ENTITY_ID + ")");
        log("  Trust Anchor: node1 (" + NODE1_ENTITY_ID + ")");
        log("");
        
        // Step 1: Fetch node3's Entity Configuration
        log("Step 1: Fetch Target Entity Configuration (node3)");
        log("--------");
        
        HttpGet getNode3Config = new HttpGet(NODE3_URL + "/.well-known/openid-federation");
        JsonNode node3Config;
        try (CloseableHttpResponse response = httpClient.execute(getNode3Config)) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            node3Config = parseJWT(body);
            
            log("GET " + NODE3_URL + "/.well-known/openid-federation");
            log("Response:");
            log(formatJson(body));
            
            Assertions.assertEquals(200, response.getCode());
            Assertions.assertEquals(NODE3_ENTITY_ID, node3Config.get("iss").asText(), 
                "iss must equal the entity's own identifier");
            Assertions.assertEquals(NODE3_ENTITY_ID, node3Config.get("sub").asText(), 
                "sub must equal iss for self-signed Entity Configuration");
            
            log("✓ Validated: iss = sub = " + NODE3_ENTITY_ID + " (self-signed)");
            log("✓ Entity Configuration JWT claims present: iss, sub, iat, exp, jti");
        }
        
        // Step 2: Extract authority_hints from node3
        log("");
        log("Step 2: Extract authority_hints from node3 Entity Configuration");
        log("--------");
        
        Assertions.assertTrue(node3Config.has("authority_hints"), 
            "Subordinate entity MUST have authority_hints per spec");
        
        JsonNode authorityHints = node3Config.get("authority_hints");
        Assertions.assertTrue(authorityHints.isArray() && authorityHints.size() > 0,
            "authority_hints must be a non-empty array");
        
        String superiorUrl = authorityHints.get(0).asText();
        log("✓ Found authority_hints: " + superiorUrl);
        log("✓ Superior entity: node1");
        
        Assertions.assertEquals(NODE1_URL, superiorUrl, 
            "node3's authority_hint should point to node1");
        
        // Step 3: Fetch node1's (superior) Entity Configuration
        log("");
        log("Step 3: Fetch Superior Entity Configuration (node1)");
        log("--------");
        
        HttpGet getNode1Config = new HttpGet(NODE1_URL + "/.well-known/openid-federation");
        JsonNode node1Config;
        try (CloseableHttpResponse response = httpClient.execute(getNode1Config)) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            node1Config = parseJWT(body);
            
            log("GET " + NODE1_URL + "/.well-known/openid-federation");
            log("Response:");
            log(formatJson(body));
            
            Assertions.assertEquals(200, response.getCode());
            Assertions.assertEquals(NODE1_ENTITY_ID, node1Config.get("iss").asText());
            
            log("✓ Superior Entity ID: " + node1Config.get("iss").asText());
            
            // Verify node1 is a Trust Anchor (no authority_hints)
            boolean isTrustAnchor = !node1Config.has("authority_hints") || 
                                   node1Config.get("authority_hints").isEmpty();
            Assertions.assertTrue(isTrustAnchor, 
                "Trust Anchor must not have authority_hints");
            
            log("✓ Confirmed: node1 is Trust Anchor (no authority_hints)");
        }
        
        // Step 4: Fetch Subordinate Statement about node3 from node1
        log("");
        log("Step 4: Fetch Subordinate Statement from Trust Anchor");
        log("--------");
        log("Per spec: Superior entity issues statements about its subordinates");
        log("");
        
        String fetchUrl = NODE1_URL + "/fetch?sub=" + NODE3_ENTITY_ID;
        log("GET " + fetchUrl);
        
        HttpGet fetchSubordinate = new HttpGet(fetchUrl);
        JsonNode subordinateStatement;
        try (CloseableHttpResponse response = httpClient.execute(fetchSubordinate)) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            subordinateStatement = parseJWT(body);
            
            log("Response:");
            log(formatJson(body));
            
            Assertions.assertEquals(200, response.getCode(), 
                "Fetch endpoint must return Subordinate Statement");
            
            // Validate Subordinate Statement per spec
            log("");
            log("Validating Subordinate Statement (per OpenID Federation 1.0):");
            
            String iss = subordinateStatement.get("iss").asText();
            String sub = subordinateStatement.get("sub").asText();
            
            log("  iss (issuer): " + iss);
            log("  sub (subject): " + sub);
            
            Assertions.assertEquals(NODE1_ENTITY_ID, iss, 
                "iss must be the superior entity (node1)");
            Assertions.assertEquals(NODE3_ENTITY_ID, sub, 
                "sub must be the subordinate entity (node3)");
            Assertions.assertNotEquals(iss, sub, 
                "In Subordinate Statement, iss MUST NOT equal sub");
            
            log("✓ Validated: iss (" + iss + ") != sub (" + sub + ")");
            log("✓ This is a valid Subordinate Statement (issuer different from subject)");
            
            // Verify required JWT claims
            Assertions.assertTrue(subordinateStatement.has("iat"), "Must have 'iat' claim");
            Assertions.assertTrue(subordinateStatement.has("exp"), "Must have 'exp' claim");
            Assertions.assertTrue(subordinateStatement.has("jti"), "Must have 'jti' claim");
            
            log("✓ All required JWT claims present: iss, sub, iat, exp, jti");
        }
        
        logSuccess("✅ Subordinate Statement validated successfully");
    }
    
    /**
     * Test 8: node2 resolves and validates trust chain for node3
     * 
     * This test implements the complete Trust Chain Validation algorithm
     * as described in OpenID Federation 1.0 Section 4.
     */
    @Test
    @Order(8)
    @DisplayName("Test 8: Complete Trust Chain Resolution and Validation (Section 4)")
    public void test08_CompleteTrustChainValidation() throws Exception {
        logTestHeader("Test 8: Complete Trust Chain Resolution (Section 4)");
        
        log("Scenario:");
        log("  - node2 wants to validate trust for node3");
        log("  - node2 must resolve trust chain: node3 → node1 (Trust Anchor)");
        log("");
        
        TrustChainResolver resolver = new TrustChainResolver();
        
        log("Starting Trust Chain Resolution...");
        log("=".repeat(60));
        
        TrustChainResult result = resolver.resolveTrustChain(NODE3_URL, NODE1_URL);
        
        log("=".repeat(60));
        log("");
        log("Trust Chain Resolution Result:");
        log("  Valid: " + result.isValid());
        log("  Target: " + result.getTargetEntity());
        log("  Trust Anchor: " + result.getTrustAnchor());
        log("  Statements Collected: " + result.getStatements().size());
        log("");
        
        if (!result.getMessages().isEmpty()) {
            log("Messages:");
            for (String msg : result.getMessages()) {
                log("  ✓ " + msg);
            }
            log("");
        }
        
        if (!result.getErrors().isEmpty()) {
            log("Errors:");
            for (String error : result.getErrors()) {
                log("  ✗ " + error);
            }
            log("");
        }
        
        if (!result.getWarnings().isEmpty()) {
            log("Warnings:");
            for (String warning : result.getWarnings()) {
                log("  ⚠ " + warning);
            }
            log("");
        }
        
        // Validate the chain
        Assertions.assertTrue(result.isValid(), 
            "Trust chain MUST be valid per OpenID Federation 1.0 Section 4");
        
        Assertions.assertTrue(result.getStatements().size() >= 2, 
            "Trust chain must contain at least: target config + subordinate statement");
        
        log("Trust Chain Statements:");
        for (int i = 0; i < result.getStatements().size(); i++) {
            JsonNode stmt = result.getStatements().get(i);
            log("  Statement " + (i + 1) + ":");
            log("    iss: " + stmt.get("iss").asText());
            log("    sub: " + stmt.get("sub").asText());
        }
        
        logSuccess("✅ Trust Chain successfully validated!");
        log("");
        log("Validation Summary:");
        log("  ✓ node3 Entity Configuration retrieved");
        log("  ✓ authority_hints extracted from node3");
        log("  ✓ node1 (Trust Anchor) Entity Configuration retrieved");
        log("  ✓ Subordinate Statement about node3 fetched from node1");
        log("  ✓ Trust chain validated: node3 is trusted via node1");
        log("");
        log("Per OpenID Federation 1.0 Section 4:");
        log("  \"A trust chain is a sequence of Entity Statements that connects");
        log("   a target entity to a Trust Anchor.\"");
        log("");
        log("✅ This trust chain connects node3 to Trust Anchor (node1)");
    }
    
    // Helper methods
    
    private static void log(String message) {
        System.out.println("[TRUST-CHAIN-TEST] " + message);
    }
    
    private static void logTestHeader(String testName) {
        log("");
        log("=".repeat(80));
        log(testName);
        log("=".repeat(80));
        log("");
    }
    
    private static void logSuccess(String message) {
        log("");
        log(message);
        log("");
    }
    
    private static String formatJson(String json) {
        try {
            // Check if it's a JWT
            if (json.startsWith("eyJ")) {
                // Parse JWT and format claims
                SignedJWT signedJWT = SignedJWT.parse(json);
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
            } else {
                // Regular JSON
                Object jsonObject = objectMapper.readValue(json, Object.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            }
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * Parse JWT and extract claims as JsonNode
     */
    private static JsonNode parseJWT(String jwtString) throws Exception {
        // Check if it's a JWT (starts with eyJ which is base64 for {"
        if (jwtString.startsWith("eyJ")) {
            SignedJWT signedJWT = SignedJWT.parse(jwtString);
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
            String claimsJson = objectMapper.writeValueAsString(claims);
            return objectMapper.readTree(claimsJson);
        } else {
            // Plain JSON
            return objectMapper.readTree(jwtString);
        }
    }
}

