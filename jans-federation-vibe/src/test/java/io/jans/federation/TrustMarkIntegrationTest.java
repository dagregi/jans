package io.jans.federation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.federation.service.TrustChainResolver;
import io.jans.federation.service.TrustChainResolver.TrustMarkValidationResult;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for Trust Marks in OpenID Federation 1.0
 * 
 * This test validates the complete Trust Mark lifecycle:
 * 1. Trust Mark Issuance
 * 2. Trust Mark Storage
 * 3. Trust Mark Inclusion in Entity Configuration
 * 4. Trust Mark Validation during Trust Chain Resolution
 * 
 * Scenario (based on Appendix A):
 * - eduGAIN (Trust Anchor) issues Trust Mark "https://refeds.org/sirtfi" to OP.UMU
 * - LIGO (Relying Party) resolves trust chain for OP.UMU
 * - LIGO validates the Trust Mark as part of trust chain resolution
 * 
 * Reference: OpenID Federation 1.0 Section 5 (Trust Marks)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrustMarkIntegrationTest {
    
    private static final String EDUGAIN_URL = "http://localhost:8080";
    private static final String SWAMID_URL = "http://localhost:8081";
    private static final String UMU_URL = "http://localhost:8082";
    private static final String OP_UMU_URL = "http://localhost:8083";
    private static final String LIGO_URL = "http://localhost:8084";
    
    private static final String EDUGAIN_ID = "https://edugain.geant.org";
    private static final String SWAMID_ID = "https://swamid.se";
    private static final String UMU_ID = "https://umu.se";
    private static final String OP_UMU_ID = "https://op.umu.se";
    private static final String LIGO_ID = "https://ligo.example.org";
    
    private static final String TRUST_MARK_SIRTFI = "https://refeds.org/sirtfi";
    
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public void setup() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Trust Mark Integration Test - Setup");
        System.out.println("=".repeat(80));
        
        // Wait for all nodes to be ready
        System.out.println("Waiting for all nodes to be ready...");
        Thread.sleep(5000);
        
        // Verify all nodes are running
        verifyNodeRunning(EDUGAIN_URL, "eduGAIN");
        verifyNodeRunning(SWAMID_URL, "SWAMID");
        verifyNodeRunning(UMU_URL, "UMU");
        verifyNodeRunning(OP_UMU_URL, "OP.UMU");
        verifyNodeRunning(LIGO_URL, "LIGO");
        
        System.out.println("✓ All nodes are running\n");
    }
    
    @Test
    @Order(1)
    public void test01_SetupFederationHierarchy() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Test 1: Setup Federation Hierarchy");
        System.out.println("=".repeat(80));
        
        // Setup hierarchy as per Appendix A
        // eduGAIN (8080) -> SWAMID (8081) -> UMU (8082) -> OP.UMU (8083)
        
        // 1. SWAMID sets eduGAIN as authority
        setAuthorityHints(SWAMID_URL, List.of(EDUGAIN_ID));
        System.out.println("✓ SWAMID -> eduGAIN authority set");
        
        // 2. eduGAIN adds SWAMID as subordinate
        addSubordinate(EDUGAIN_URL, SWAMID_ID);
        System.out.println("✓ eduGAIN added SWAMID as subordinate");
        
        // 3. UMU sets SWAMID as authority
        setAuthorityHints(UMU_URL, List.of(SWAMID_ID));
        System.out.println("✓ UMU -> SWAMID authority set");
        
        // 4. SWAMID adds UMU as subordinate
        addSubordinate(SWAMID_URL, UMU_ID);
        System.out.println("✓ SWAMID added UMU as subordinate");
        
        // 5. OP.UMU sets UMU as authority
        setAuthorityHints(OP_UMU_URL, List.of(UMU_ID));
        System.out.println("✓ OP.UMU -> UMU authority set");
        
        // 6. UMU adds OP.UMU as subordinate
        addSubordinate(UMU_URL, OP_UMU_ID);
        System.out.println("✓ UMU added OP.UMU as subordinate");
        
        // 7. LIGO sets eduGAIN as Trust Anchor (for validation)
        setAuthorityHints(LIGO_URL, List.of(EDUGAIN_ID));
        System.out.println("✓ LIGO configured with eduGAIN as Trust Anchor");
        
        System.out.println("\n✓ Federation hierarchy setup complete");
    }
    
    @Test
    @Order(2)
    public void test02_IssueTrustMarkToOPUMU() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Test 2: Issue Trust Mark from eduGAIN to OP.UMU");
        System.out.println("=".repeat(80));
        
        // eduGAIN (Trust Anchor) issues SIRTFI Trust Mark to OP.UMU
        System.out.println("Issuing Trust Mark:");
        System.out.println("  Issuer: eduGAIN (https://edugain.geant.org)");
        System.out.println("  Subject: OP.UMU (https://op.umu.se)");
        System.out.println("  Trust Mark ID: " + TRUST_MARK_SIRTFI);
        
        Map<String, Object> request = new HashMap<>();
        request.put("trust_mark_id", TRUST_MARK_SIRTFI);
        request.put("subject", OP_UMU_ID);
        request.put("expires_in", 31536000); // 1 year
        
        String requestJson = objectMapper.writeValueAsString(request);
        HttpPost post = new HttpPost(EDUGAIN_URL + "/manage/trust-marks");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            System.out.println("Response Status: " + statusCode);
            System.out.println("Response Body:\n" + formatJson(responseBody));
            
            assertEquals(201, statusCode, "Should return 201 Created");
            
            JsonNode responseJson = objectMapper.readTree(responseBody);
            assertTrue(responseJson.has("signed_jwt"), "Response should contain signed_jwt");
            
            String signedJWT = responseJson.get("signed_jwt").asText();
            assertNotNull(signedJWT, "Signed JWT should not be null");
            assertTrue(signedJWT.startsWith("eyJ"), "Should be a valid JWT");
            
            System.out.println("\n✓ Trust Mark issued successfully");
            System.out.println("  Signed JWT (first 100 chars): " + 
                signedJWT.substring(0, Math.min(100, signedJWT.length())) + "...");
            
            // Store the signed JWT for next test
            System.setProperty("TRUST_MARK_JWT", signedJWT);
        }
    }
    
    @Test
    @Order(3)
    public void test03_AddTrustMarkToOPUMU() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Test 3: Add Trust Mark to OP.UMU Entity");
        System.out.println("=".repeat(80));
        
        String signedJWT = System.getProperty("TRUST_MARK_JWT");
        assertNotNull(signedJWT, "Trust Mark JWT should be available from previous test");
        
        System.out.println("Adding Trust Mark to OP.UMU...");
        
        Map<String, Object> request = new HashMap<>();
        request.put("signed_jwt", signedJWT);
        
        String requestJson = objectMapper.writeValueAsString(request);
        HttpPost post = new HttpPost(OP_UMU_URL + "/manage/entity/trust-marks");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            System.out.println("Response Status: " + statusCode);
            System.out.println("Response Body:\n" + formatJson(responseBody));
            
            assertEquals(201, statusCode, "Should return 201 Created");
            
            JsonNode responseJson = objectMapper.readTree(responseBody);
            assertEquals("added", responseJson.get("status").asText());
            assertEquals(TRUST_MARK_SIRTFI, responseJson.get("trust_mark_id").asText());
            assertEquals(EDUGAIN_ID, responseJson.get("issuer").asText());
            assertEquals(OP_UMU_ID, responseJson.get("subject").asText());
            
            System.out.println("\n✓ Trust Mark added to OP.UMU successfully");
        }
    }
    
    @Test
    @Order(4)
    public void test04_VerifyTrustMarkInEntityConfiguration() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Test 4: Verify Trust Mark in OP.UMU Entity Configuration");
        System.out.println("=".repeat(80));
        
        System.out.println("Fetching OP.UMU Entity Configuration...");
        
        HttpGet get = new HttpGet(OP_UMU_URL + "/.well-known/openid-federation");
        
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            int statusCode = response.getCode();
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            System.out.println("Response Status: " + statusCode);
            assertEquals(200, statusCode, "Should return 200 OK");
            
            // Parse JWT
            JsonNode config = parseJWT(jwtString);
            System.out.println("Entity Configuration Claims:\n" + formatJson(objectMapper.writeValueAsString(config)));
            
            // Verify Trust Mark is present
            assertTrue(config.has("trust_marks"), "Entity Configuration should contain trust_marks");
            assertTrue(config.get("trust_marks").isArray(), "trust_marks should be an array");
            assertTrue(config.get("trust_marks").size() > 0, "Should have at least one Trust Mark");
            
            String trustMarkJWT = config.get("trust_marks").get(0).asText();
            System.out.println("\nTrust Mark JWT found in Entity Configuration:");
            System.out.println("  " + trustMarkJWT.substring(0, Math.min(100, trustMarkJWT.length())) + "...");
            
            // Parse Trust Mark JWT
            JsonNode trustMarkClaims = parseJWT(trustMarkJWT);
            System.out.println("\nTrust Mark Claims:\n" + formatJson(objectMapper.writeValueAsString(trustMarkClaims)));
            
            assertEquals(TRUST_MARK_SIRTFI, trustMarkClaims.get("id").asText(), "Trust Mark ID should match");
            assertEquals(EDUGAIN_ID, trustMarkClaims.get("iss").asText(), "Issuer should be eduGAIN");
            assertEquals(OP_UMU_ID, trustMarkClaims.get("sub").asText(), "Subject should be OP.UMU");
            
            System.out.println("\n✓ Trust Mark correctly included in Entity Configuration");
        }
    }
    
    @Test
    @Order(5)
    public void test05_ValidateTrustMarkDuringTrustChainResolution() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Test 5: Validate Trust Mark during Trust Chain Resolution");
        System.out.println("=".repeat(80));
        
        System.out.println("LIGO (Relying Party) resolves and validates trust chain for OP.UMU...\n");
        
        TrustChainResolver resolver = new TrustChainResolver();
        TrustChainResolver.TrustChainResult result = resolver.resolveTrustChain(OP_UMU_URL, EDUGAIN_URL);
        
        System.out.println("\nTrust Chain Resolution Result:");
        System.out.println("  Valid: " + result.isValid());
        System.out.println("  Statements in chain: " + result.getStatements().size());
        
        assertTrue(result.isValid(), "Trust chain should be valid");
        assertTrue(result.getStatements().size() >= 4, "Should have at least 4 statements in chain");
        
        // Now validate Trust Marks
        System.out.println("\n" + "-".repeat(80));
        System.out.println("Validating Trust Marks...");
        System.out.println("-".repeat(80));
        
        // Get OP.UMU's Entity Configuration (first statement)
        JsonNode opUmuConfig = result.getStatements().get(0);
        String opUmuId = opUmuConfig.get("iss").asText();
        
        List<TrustMarkValidationResult> trustMarkResults = 
            resolver.validateTrustMarks(opUmuConfig, opUmuId, result.getStatements());
        
        System.out.println("\nTrust Mark Validation Results:");
        System.out.println("  Total Trust Marks: " + trustMarkResults.size());
        
        assertFalse(trustMarkResults.isEmpty(), "Should have at least one Trust Mark");
        
        for (TrustMarkValidationResult tmResult : trustMarkResults) {
            System.out.println("\n  Trust Mark: " + tmResult.getTrustMarkId());
            System.out.println("    Issuer: " + tmResult.getIssuer());
            System.out.println("    Subject: " + tmResult.getSubject());
            System.out.println("    Valid: " + tmResult.isValid());
            if (!tmResult.isValid()) {
                System.out.println("    Error: " + tmResult.getError());
            }
            
            // Assertions
            assertEquals(TRUST_MARK_SIRTFI, tmResult.getTrustMarkId(), "Trust Mark ID should match");
            assertEquals(EDUGAIN_ID, tmResult.getIssuer(), "Issuer should be eduGAIN");
            assertEquals(OP_UMU_ID, tmResult.getSubject(), "Subject should be OP.UMU");
            assertTrue(tmResult.isValid(), "Trust Mark should be valid: " + tmResult.getError());
        }
        
        System.out.println("\n✓ Trust Mark validation successful!");
        System.out.println("✓ Trust Mark is properly signed by Trust Anchor (eduGAIN)");
        System.out.println("✓ Trust Mark issuer is in the trust chain");
        System.out.println("✓ Trust Mark subject matches OP.UMU");
        System.out.println("✓ Trust Mark signature verified successfully");
    }
    
    // Helper methods
    
    private void verifyNodeRunning(String baseUrl, String nodeName) throws Exception {
        HttpGet get = new HttpGet(baseUrl + "/manage/entity");
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            int statusCode = response.getCode();
            if (statusCode == 200) {
                System.out.println("  ✓ " + nodeName + " is running");
            } else {
                fail(nodeName + " is not responding correctly (status: " + statusCode + ")");
            }
        }
    }
    
    private void setAuthorityHints(String baseUrl, List<String> hints) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("authority_hints", hints);
        
        String requestJson = objectMapper.writeValueAsString(request);
        HttpPost post = new HttpPost(baseUrl + "/manage/entity/authority-hints");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            if (statusCode != 200) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                fail("Failed to set authority hints: " + body);
            }
        }
    }
    
    private void addSubordinate(String baseUrl, String subordinateId) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("entity_id", subordinateId);
        
        String requestJson = objectMapper.writeValueAsString(request);
        HttpPost post = new HttpPost(baseUrl + "/manage/subordinates");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            // Accept both 200 (updated) and 201 (created)
            if (statusCode != 200 && statusCode != 201) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                fail("Failed to add subordinate: " + body);
            }
        }
    }
    
    private JsonNode parseJWT(String jwt) throws Exception {
        com.nimbusds.jwt.SignedJWT signedJWT = com.nimbusds.jwt.SignedJWT.parse(jwt);
        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
        String claimsJson = objectMapper.writeValueAsString(claims);
        return objectMapper.readTree(claimsJson);
    }
    
    private String formatJson(String json) {
        try {
            // Try to parse as JWT first
            if (json.startsWith("eyJ")) {
                JsonNode parsed = parseJWT(json);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
            }
            // Otherwise parse as plain JSON
            JsonNode parsed = objectMapper.readTree(json);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return json;
        }
    }
}

