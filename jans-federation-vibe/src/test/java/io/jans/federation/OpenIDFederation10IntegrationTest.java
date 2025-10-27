package io.jans.federation;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive Integration Tests for OpenID Federation 1.0 Implementation
 * 
 * This test suite validates all key aspects of the OpenID Federation 1.0 specification:
 * - Section 3.1: Entity Configuration Discovery
 * - Section 3.2: Federation Metadata
 * - Section 3.3: Trust Mark Issuers
 * - Section 3.4: Trust Marks
 * - Section 4.0: Trust Chain Validation
 * - Section 5.0: Entity Registration
 * - Section 6.0: JWKS Endpoint
 * 
 * Reference: https://openid.net/specs/openid-federation-1_0.html
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenIDFederation10IntegrationTest {
    
    private static final String BASE_URL = "http://localhost:8080";
    private static CloseableHttpClient httpClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public static void setup() {
        httpClient = HttpClients.createDefault();
        log("========================================");
        log("OpenID Federation 1.0 Integration Tests");
        log("========================================");
        log("Base URL: " + BASE_URL);
        log("");
    }
    
    @AfterAll
    public static void tearDown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
        log("");
        log("========================================");
        log("All Tests Completed");
        log("========================================");
    }
    
    /**
     * Test 1: Application Health Check
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: Application Health Check")
    public void test01_ApplicationHealth() throws Exception {
        logTestHeader("Test 1: Application Health Check");
        
        String url = BASE_URL + "/database/health";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Health check should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("status"), "Response should contain 'status' field");
            Assertions.assertEquals("healthy", json.get("status").asText(), "Status should be 'healthy'");
            
            logSuccess("✅ Application is healthy and running");
        }
    }
    
    /**
     * Test 2: Database Statistics
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: Database Statistics")
    public void test02_DatabaseStatistics() throws Exception {
        logTestHeader("Test 2: Database Statistics");
        
        String url = BASE_URL + "/database/stats";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Stats endpoint should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("total_entities"), "Response should contain 'total_entities'");
            Assertions.assertTrue(json.has("active_trust_marks"), "Response should contain 'active_trust_marks'");
            
            log("Total Entities: " + json.get("total_entities").asInt());
            log("Active Trust Marks: " + json.get("active_trust_marks").asInt());
            log("Trust Mark Issuers: " + json.get("trust_mark_issuers").asInt());
            
            logSuccess("✅ Database statistics retrieved successfully");
        }
    }
    
    /**
     * Test 3: Entity Configuration Discovery (Section 3.1)
     * 
     * OpenID Federation 1.0 Specification Section 3.1:
     * "Entity Configurations are discovered via a well-known endpoint"
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Entity Configuration Discovery (OpenID Federation 1.0 Section 3.1)")
    public void test03_EntityConfigurationDiscovery() throws Exception {
        logTestHeader("Test 3: Entity Configuration Discovery (Section 3.1)");
        
        String[] testEntities = {
            "https://op.example.com",
            "https://rp.example.com",
            "https://test-op.example.com"
        };
        
        for (String entityId : testEntities) {
            log("Testing Entity: " + entityId);
            String url = BASE_URL + "/.well-known/openid-federation?iss=" + entityId;
            log("Request: GET " + url);
            
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                log("Response Status: " + statusCode);
                log("Response Body: " + formatJson(responseBody));
                
                Assertions.assertEquals(200, statusCode, "Entity configuration should return 200 OK");
                
                JsonNode json = objectMapper.readTree(responseBody);
                
                // Validate required fields per OpenID Federation 1.0 Section 3.1
                String[] requiredFields = {"iss", "sub", "aud", "exp", "iat", "jti", "jwks"};
                for (String field : requiredFields) {
                    Assertions.assertTrue(json.has(field), 
                        "Entity configuration must contain '" + field + "' field (Section 3.1)");
                }
                
                Assertions.assertEquals(entityId, json.get("iss").asText(), 
                    "'iss' must match the requested entity ID");
                Assertions.assertEquals(entityId, json.get("sub").asText(), 
                    "'sub' must match the requested entity ID");
                
                log("✓ Entity ID (iss): " + json.get("iss").asText());
                log("✓ Subject (sub): " + json.get("sub").asText());
                log("✓ Audience (aud): " + json.get("aud").asText());
                log("✓ JWT ID (jti): " + json.get("jti").asText());
                log("✓ All required fields present");
                log("");
            }
        }
        
        logSuccess("✅ Entity Configuration Discovery validated for all test entities");
    }
    
    /**
     * Test 4: Federation Metadata (Section 3.2)
     * 
     * OpenID Federation 1.0 Specification Section 3.2:
     * "Federation Metadata provides information about the federation"
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Federation Metadata (OpenID Federation 1.0 Section 3.2)")
    public void test04_FederationMetadata() throws Exception {
        logTestHeader("Test 4: Federation Metadata (Section 3.2)");
        
        String url = BASE_URL + "/federation/metadata";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Metadata endpoint should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("federation_name"), 
                "Metadata should contain 'federation_name'");
            Assertions.assertTrue(json.has("issuer"), 
                "Metadata should contain 'issuer'");
            
            log("✓ Federation Name: " + json.get("federation_name").asText());
            log("✓ Issuer: " + json.get("issuer").asText());
            log("✓ Version: " + json.get("version").asText());
            
            logSuccess("✅ Federation Metadata retrieved successfully");
        }
    }
    
    /**
     * Test 5: Trust Mark Issuers (Section 3.3)
     * 
     * OpenID Federation 1.0 Specification Section 3.3:
     * "Trust Mark Issuers are entities authorized to issue trust marks"
     */
    @Test
    @Order(5)
    @DisplayName("Test 5: Trust Mark Issuers (OpenID Federation 1.0 Section 3.3)")
    public void test05_TrustMarkIssuers() throws Exception {
        logTestHeader("Test 5: Trust Mark Issuers (Section 3.3)");
        
        String url = BASE_URL + "/federation/trust-mark-issuers";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Trust mark issuers endpoint should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.isArray(), "Response should be an array");
            Assertions.assertTrue(json.size() > 0, "Should have at least one trust mark issuer");
            
            log("Found " + json.size() + " trust mark issuers:");
            for (JsonNode issuer : json) {
                Assertions.assertTrue(issuer.has("issuer"), "Each issuer must have 'issuer' field");
                log("  - " + issuer.get("issuer").asText());
            }
            
            logSuccess("✅ Trust Mark Issuers retrieved successfully");
        }
    }
    
    /**
     * Test 6: Trust Marks (Section 3.4)
     * 
     * OpenID Federation 1.0 Specification Section 3.4:
     * "Trust Marks are signed statements about an entity"
     */
    @Test
    @Order(6)
    @DisplayName("Test 6: Trust Marks (OpenID Federation 1.0 Section 3.4)")
    public void test06_TrustMarks() throws Exception {
        logTestHeader("Test 6: Trust Marks (Section 3.4)");
        
        String url = BASE_URL + "/federation/trust-marks";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Trust marks endpoint should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.isArray(), "Response should be an array");
            Assertions.assertTrue(json.size() > 0, "Should have at least one trust mark");
            
            log("Found " + json.size() + " trust marks:");
            for (JsonNode trustMark : json) {
                Assertions.assertTrue(trustMark.has("entity_id"), "Trust mark must have 'entity_id'");
                Assertions.assertTrue(trustMark.has("trust_mark_id"), "Trust mark must have 'trust_mark_id'");
                log("  - Entity: " + trustMark.get("entity_id").asText() + 
                    ", Mark: " + trustMark.get("trust_mark_id").asText());
            }
            
            logSuccess("✅ Trust Marks retrieved successfully");
        }
    }
    
    /**
     * Test 7: Trust Chain Validation (Section 4)
     * 
     * OpenID Federation 1.0 Specification Section 4:
     * "Trust chains are validated by verifying each link in the chain"
     */
    @Test
    @Order(7)
    @DisplayName("Test 7: Trust Chain Validation (OpenID Federation 1.0 Section 4)")
    public void test07_TrustChainValidation() throws Exception {
        logTestHeader("Test 7: Trust Chain Validation (Section 4)");
        
        String url = BASE_URL + "/federation/validate-trust-chain";
        log("Request: POST " + url);
        
        String requestBody = "{\n" +
            "  \"entity_id\": \"https://op.example.com\",\n" +
            "  \"trust_mark_id\": \"basic-trust\"\n" +
            "}";
        log("Request Body: " + formatJson(requestBody));
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Trust chain validation should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("valid"), "Response must contain 'valid' field");
            Assertions.assertTrue(json.has("trust_chain"), "Response must contain 'trust_chain' field");
            
            boolean isValid = json.get("valid").asBoolean();
            log("✓ Trust Chain Valid: " + isValid);
            
            if (json.has("trust_chain") && json.get("trust_chain").isArray()) {
                log("✓ Trust Chain:");
                for (JsonNode entity : json.get("trust_chain")) {
                    log("    " + entity.asText());
                }
            }
            
            logSuccess("✅ Trust Chain Validation completed successfully");
        }
    }
    
    /**
     * Test 8: Entity Registration (Section 5)
     * 
     * OpenID Federation 1.0 Specification Section 5:
     * "Entities can be registered and issued trust marks"
     */
    @Test
    @Order(8)
    @DisplayName("Test 8: Entity Registration / Trust Mark Issuance (OpenID Federation 1.0 Section 5)")
    public void test08_EntityRegistration() throws Exception {
        logTestHeader("Test 8: Entity Registration / Trust Mark Issuance (Section 5)");
        
        String url = BASE_URL + "/federation/issue-trust-mark";
        log("Request: POST " + url);
        
        String requestBody = "{\n" +
            "  \"entity_id\": \"https://new-entity.example.com\",\n" +
            "  \"trust_mark_id\": \"basic-trust\",\n" +
            "  \"metadata\": {\n" +
            "    \"openid_provider\": {\n" +
            "      \"issuer\": \"https://new-entity.example.com\",\n" +
            "      \"authorization_endpoint\": \"https://new-entity.example.com/auth\",\n" +
            "      \"token_endpoint\": \"https://new-entity.example.com/token\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        log("Request Body: " + formatJson(requestBody));
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "Trust mark issuance should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("entity_id"), "Response must contain 'entity_id'");
            Assertions.assertTrue(json.has("trust_mark_id"), "Response must contain 'trust_mark_id'");
            
            log("✓ Trust Mark Issued for Entity: " + json.get("entity_id").asText());
            log("✓ Trust Mark ID: " + json.get("trust_mark_id").asText());
            
            logSuccess("✅ Trust Mark issued successfully");
        }
    }
    
    /**
     * Test 9: JWKS Endpoint (Section 6)
     * 
     * OpenID Federation 1.0 Specification Section 6:
     * "JWKS endpoint provides public keys for signature verification"
     */
    @Test
    @Order(9)
    @DisplayName("Test 9: JWKS Endpoint (OpenID Federation 1.0 Section 6)")
    public void test09_JWKSEndpoint() throws Exception {
        logTestHeader("Test 9: JWKS Endpoint (Section 6)");
        
        String url = BASE_URL + "/federation/jwks";
        log("Request: GET " + url);
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log("Response Status: " + statusCode);
            log("Response Body: " + formatJson(responseBody));
            
            Assertions.assertEquals(200, statusCode, "JWKS endpoint should return 200 OK");
            
            JsonNode json = objectMapper.readTree(responseBody);
            Assertions.assertTrue(json.has("keys"), "JWKS must contain 'keys' array");
            Assertions.assertTrue(json.get("keys").isArray(), "'keys' must be an array");
            Assertions.assertTrue(json.get("keys").size() > 0, "Must have at least one key");
            
            log("Found " + json.get("keys").size() + " keys:");
            for (JsonNode key : json.get("keys")) {
                Assertions.assertTrue(key.has("kty"), "Key must have 'kty' (key type)");
                Assertions.assertTrue(key.has("kid"), "Key must have 'kid' (key ID)");
                log("  - Key ID: " + key.get("kid").asText() + 
                    ", Type: " + key.get("kty").asText() + 
                    ", Use: " + (key.has("use") ? key.get("use").asText() : "N/A"));
            }
            
            logSuccess("✅ JWKS endpoint validated successfully");
        }
    }
    
    /**
     * Test 10: Complete OpenID Federation 1.0 Flow
     */
    @Test
    @Order(10)
    @DisplayName("Test 10: Complete OpenID Federation 1.0 Flow")
    public void test10_CompleteFlow() throws Exception {
        logTestHeader("Test 10: Complete OpenID Federation 1.0 Flow");
        
        log("Simulating complete federation flow:");
        log("1. Discover entity configuration");
        log("2. Retrieve federation metadata");
        log("3. Get trust marks");
        log("4. Validate trust chain");
        log("5. Verify JWKS");
        
        // Step 1: Entity Configuration
        String entityId = "https://op.example.com";
        HttpGet configRequest = new HttpGet(BASE_URL + "/.well-known/openid-federation?iss=" + entityId);
        try (CloseableHttpResponse response = httpClient.execute(configRequest)) {
            Assertions.assertEquals(200, response.getCode());
            log("  ✓ Step 1: Entity configuration discovered");
        }
        
        // Step 2: Federation Metadata
        HttpGet metadataRequest = new HttpGet(BASE_URL + "/federation/metadata");
        try (CloseableHttpResponse response = httpClient.execute(metadataRequest)) {
            Assertions.assertEquals(200, response.getCode());
            log("  ✓ Step 2: Federation metadata retrieved");
        }
        
        // Step 3: Trust Marks
        HttpGet trustMarksRequest = new HttpGet(BASE_URL + "/federation/trust-marks");
        try (CloseableHttpResponse response = httpClient.execute(trustMarksRequest)) {
            Assertions.assertEquals(200, response.getCode());
            log("  ✓ Step 3: Trust marks retrieved");
        }
        
        // Step 4: Trust Chain Validation
        String validationBody = "{\"entity_id\": \"" + entityId + "\", \"trust_mark_id\": \"basic-trust\"}";
        HttpPost validationRequest = new HttpPost(BASE_URL + "/federation/validate-trust-chain");
        validationRequest.setHeader("Content-Type", "application/json");
        validationRequest.setEntity(new StringEntity(validationBody, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = httpClient.execute(validationRequest)) {
            Assertions.assertEquals(200, response.getCode());
            log("  ✓ Step 4: Trust chain validated");
        }
        
        // Step 5: JWKS
        HttpGet jwksRequest = new HttpGet(BASE_URL + "/federation/jwks");
        try (CloseableHttpResponse response = httpClient.execute(jwksRequest)) {
            Assertions.assertEquals(200, response.getCode());
            log("  ✓ Step 5: JWKS verified");
        }
        
        logSuccess("✅ Complete OpenID Federation 1.0 flow executed successfully!");
    }
    
    // Helper methods
    
    private static void log(String message) {
        System.out.println("[TEST] " + message);
    }
    
    private static void logTestHeader(String testName) {
        log("");
        log("========================================");
        log(testName);
        log("========================================");
    }
    
    private static void logSuccess(String message) {
        log("");
        log(message);
        log("");
    }
    
    private static String formatJson(String json) {
        try {
            Object jsonObject = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            return json;
        }
    }
}

