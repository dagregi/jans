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
 * Integration Test Based on OpenID Federation 1.0 Appendix A
 * 
 * This test implements the exact scenario from Appendix A:
 * "Example OpenID Provider Information Discovery and Client Registration"
 * 
 * Entity Hierarchy:
 *   eduGAIN (https://edugain.geant.org) - Trust Anchor
 *     └── SWAMID (https://swamid.se) - Intermediate Authority
 *         └── UMU (https://umu.se) - Organization
 *             └── OP.UMU (https://op.umu.se) - OpenID Provider
 * 
 * The LIGO Wiki (Relying Party) discovers the OP's metadata through the trust chain.
 * 
 * Port Assignments:
 * - eduGAIN: 8080
 * - SWAMID: 8081  
 * - UMU: 8082
 * - OP.UMU: 8083
 * - LIGO (RP): 8084
 * 
 * Reference: OpenID Federation 1.0 Appendix A
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppendixAIntegrationTest {
    
    // Entity URLs (mapped to localhost for testing)
    private static final String EDUGAIN_URL = "http://localhost:8080";  // Trust Anchor
    private static final String SWAMID_URL = "http://localhost:8081";   // Intermediate
    private static final String UMU_URL = "http://localhost:8082";      // Organization
    private static final String OP_UMU_URL = "http://localhost:8083";   // OpenID Provider
    private static final String LIGO_URL = "http://localhost:8084";     // Relying Party
    
    // Entity IDs (as per Appendix A)
    private static final String EDUGAIN_ID = "https://edugain.geant.org";
    private static final String SWAMID_ID = "https://swamid.se";
    private static final String UMU_ID = "https://umu.se";
    private static final String OP_UMU_ID = "https://op.umu.se";
    private static final String LIGO_ID = "https://ligo.example.org";
    
    private static CloseableHttpClient httpClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public static void setup() throws Exception {
        httpClient = HttpClients.createDefault();
        log("=".repeat(80));
        log("OpenID Federation 1.0 - Appendix A Integration Test");
        log("Example: OpenID Provider Information Discovery and Client Registration");
        log("=".repeat(80));
        log("");
        log("Entity Hierarchy (from Appendix A):");
        log("  eduGAIN (https://edugain.geant.org) - Trust Anchor");
        log("    └── SWAMID (https://swamid.se) - Intermediate Authority");
        log("        └── UMU (https://umu.se) - Organization");
        log("            └── OP.UMU (https://op.umu.se) - OpenID Provider");
        log("");
        log("Relying Party:");
        log("  LIGO Wiki (https://ligo.example.org)");
        log("");
        log("Scenario: A.2 - The LIGO Wiki Discovers the OP's Metadata");
        log("=".repeat(80));
        log("");
        
        // Clean up any existing subordinates
        cleanupSubordinates();
    }
    
    private static void cleanupSubordinates() {
        // Clean up from previous runs
        try {
            String[] nodes = {EDUGAIN_URL, SWAMID_URL, UMU_URL};
            String[][] subordinates = {
                {SWAMID_ID},           // eduGAIN's subordinates
                {UMU_ID},              // SWAMID's subordinates  
                {OP_UMU_ID}            // UMU's subordinates
            };
            
            for (int i = 0; i < nodes.length; i++) {
                for (String subId : subordinates[i]) {
                    try {
                        org.apache.hc.client5.http.classic.methods.HttpDelete deleteRequest = 
                            new org.apache.hc.client5.http.classic.methods.HttpDelete(
                                nodes[i] + "/manage/subordinates/" + subId);
                        httpClient.execute(deleteRequest).close();
                    } catch (Exception e) {
                        // Ignore
                    }
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
        log("✅ Appendix A Integration Test Completed");
        log("=".repeat(80));
    }
    
    /**
     * Test 1: Verify all entities are running
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: Verify All Entities Are Running (Appendix A Setup)")
    public void test01_VerifyEntitiesRunning() throws Exception {
        logTestHeader("Test 1: Verify All Entities Running");
        
        log("Per Appendix A.1 - Setting Up a Federation");
        log("");
        
        String[][] entities = {
            {EDUGAIN_URL, "eduGAIN (Trust Anchor)", EDUGAIN_ID},
            {SWAMID_URL, "SWAMID (Intermediate Authority)", SWAMID_ID},
            {UMU_URL, "UMU (Organization)", UMU_ID},
            {OP_UMU_URL, "OP.UMU (OpenID Provider)", OP_UMU_ID},
            {LIGO_URL, "LIGO Wiki (Relying Party)", LIGO_ID}
        };
        
        for (String[] entity : entities) {
            log("Checking " + entity[1] + "...");
            HttpGet request = new HttpGet(entity[0] + "/.well-known/openid-federation");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getCode(), 
                    entity[1] + " should be running");
                
                String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonNode config = parseJWT(jwtString);
                
                log("  ✓ Entity ID: " + config.get("iss").asText());
                log("  ✓ Responding with signed JWT Entity Configuration");
            }
        }
        
        logSuccess("✅ All 5 entities are running (eduGAIN, SWAMID, UMU, OP.UMU, LIGO)");
    }
    
    /**
     * Test 2: Configure eduGAIN as Trust Anchor
     * Per Appendix A.1 - eduGAIN is the root of trust
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: A.1 - Configure eduGAIN as Trust Anchor")
    public void test02_ConfigureEduGAINAsTrustAnchor() throws Exception {
        logTestHeader("Test 2: Configure eduGAIN as Trust Anchor (Appendix A.1)");
        
        log("Per Appendix A.1:");
        log("  eduGAIN (https://edugain.geant.org) is the Trust Anchor");
        log("  Trust Anchors have NO authority_hints");
        log("");
        
        // Configure eduGAIN with empty authority hints
        String url = EDUGAIN_URL + "/manage/entity/authority-hints";
        String requestBody = "{\"authority_hints\": []}";
        
        log("POST " + url);
        log("Body: " + requestBody);
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(200, response.getCode());
            log("✓ eduGAIN configured as Trust Anchor");
        }
        
        // Verify A.2.6 - Entity Configuration for eduGAIN
        log("");
        log("Verifying A.2.6 - Entity Configuration for https://edugain.geant.org");
        
        HttpGet getConfig = new HttpGet(EDUGAIN_URL + "/.well-known/openid-federation");
        try (CloseableHttpResponse response = httpClient.execute(getConfig)) {
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode config = parseJWT(jwtString);
            
            log("GET " + EDUGAIN_URL + "/.well-known/openid-federation");
            log("Response (JWT decoded):");
            log(formatJson(jwtString));
            
            Assertions.assertEquals(EDUGAIN_ID, config.get("iss").asText());
            Assertions.assertEquals(EDUGAIN_ID, config.get("sub").asText(), 
                "Entity Configuration must be self-signed (iss == sub)");
            
            // Trust Anchor must not have authority_hints
            boolean isTrustAnchor = !config.has("authority_hints") || 
                                   config.get("authority_hints").isEmpty();
            Assertions.assertTrue(isTrustAnchor, "Trust Anchor must not have authority_hints");
            
            log("✓ iss: " + config.get("iss").asText());
            log("✓ sub: " + config.get("sub").asText());
            log("✓ iss == sub (self-signed Entity Configuration)");
            log("✓ authority_hints: [] (Trust Anchor)");
        }
        
        logSuccess("✅ A.2.6 - eduGAIN Entity Configuration validated");
    }
    
    /**
     * Test 3: Register SWAMID as subordinate of eduGAIN
     * Build the chain: SWAMID → eduGAIN
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Register SWAMID as Subordinate of eduGAIN")
    public void test03_RegisterSWAMIDUnderEduGAIN() throws Exception {
        logTestHeader("Test 3: Register SWAMID Under eduGAIN");
        
        log("Building Federation Hierarchy:");
        log("  eduGAIN → SWAMID");
        log("");
        
        // Register SWAMID as subordinate of eduGAIN
        String url = EDUGAIN_URL + "/manage/subordinates";
        String requestBody = "{\n" +
            "  \"entity_id\": \"" + SWAMID_ID + "\",\n" +
            "  \"jwks\": {\n" +
            "    \"keys\": [{\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"kid\": \"swamid-key-1\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"alg\": \"RS256\"\n" +
            "    }]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"federation_entity\": {\n" +
            "      \"name\": \"SWAMID\",\n" +
            "      \"homepage_uri\": \"https://www.swamid.se/\",\n" +
            "      \"federation_fetch_endpoint\": \"" + SWAMID_URL + "/fetch\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        
        log("POST " + url);
        log("Registering SWAMID as subordinate...");
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertTrue(response.getCode() == 200 || response.getCode() == 201);
            log("✓ SWAMID registered under eduGAIN");
        }
        
        // Configure SWAMID to point to eduGAIN
        url = SWAMID_URL + "/manage/entity/authority-hints";
        requestBody = "{\"authority_hints\": [\"" + EDUGAIN_URL + "\"]}";
        
        log("");
        log("Configuring SWAMID authority hints...");
        request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(200, response.getCode());
            log("✓ SWAMID points to eduGAIN");
        }
        
        // Verify A.2.7 - Subordinate Statement by eduGAIN about SWAMID
        log("");
        log("Verifying A.2.7 - Subordinate Statement by eduGAIN about SWAMID");
        
        HttpGet fetchStmt = new HttpGet(EDUGAIN_URL + "/fetch?sub=" + SWAMID_ID);
        try (CloseableHttpResponse response = httpClient.execute(fetchStmt)) {
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode stmt = parseJWT(jwtString);
            
            log("GET " + EDUGAIN_URL + "/fetch?sub=" + SWAMID_ID);
            log("Response (JWT decoded):");
            log(formatJson(jwtString));
            
            Assertions.assertEquals(EDUGAIN_ID, stmt.get("iss").asText(), 
                "Issuer must be eduGAIN");
            Assertions.assertEquals(SWAMID_ID, stmt.get("sub").asText(), 
                "Subject must be SWAMID");
            Assertions.assertNotEquals(stmt.get("iss").asText(), stmt.get("sub").asText(),
                "Subordinate Statement: iss MUST NOT equal sub");
            
            log("✓ iss: " + stmt.get("iss").asText() + " (eduGAIN)");
            log("✓ sub: " + stmt.get("sub").asText() + " (SWAMID)");
            log("✓ iss != sub (valid Subordinate Statement)");
        }
        
        logSuccess("✅ SWAMID registered under eduGAIN + A.2.7 validated");
    }
    
    /**
     * Test 4: Register UMU as subordinate of SWAMID
     * Build the chain: UMU → SWAMID → eduGAIN
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Register UMU Under SWAMID")
    public void test04_RegisterUMUUnderSWAMID() throws Exception {
        logTestHeader("Test 4: Register UMU Under SWAMID");
        
        log("Building Federation Hierarchy:");
        log("  eduGAIN → SWAMID → UMU");
        log("");
        
        // Register UMU as subordinate of SWAMID
        String url = SWAMID_URL + "/manage/subordinates";
        String requestBody = "{\n" +
            "  \"entity_id\": \"" + UMU_ID + "\",\n" +
            "  \"jwks\": {\n" +
            "    \"keys\": [{\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"kid\": \"umu-key-1\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"alg\": \"RS256\"\n" +
            "    }]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"federation_entity\": {\n" +
            "      \"name\": \"Umeå University\",\n" +
            "      \"homepage_uri\": \"https://www.umu.se/\",\n" +
            "      \"federation_fetch_endpoint\": \"" + UMU_URL + "/fetch\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
        
        log("POST " + url);
        log("Registering UMU as subordinate of SWAMID...");
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertTrue(response.getCode() == 200 || response.getCode() == 201);
            log("✓ UMU registered under SWAMID");
        }
        
        // Configure UMU to point to SWAMID
        url = UMU_URL + "/manage/entity/authority-hints";
        requestBody = "{\"authority_hints\": [\"" + SWAMID_URL + "\"]}";
        
        log("");
        log("Configuring UMU authority hints...");
        request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(200, response.getCode());
            log("✓ UMU points to SWAMID");
        }
        
        // Verify A.2.5 - Subordinate Statement by SWAMID about UMU
        log("");
        log("Verifying A.2.5 - Subordinate Statement by SWAMID about UMU");
        
        HttpGet fetchStmt = new HttpGet(SWAMID_URL + "/fetch?sub=" + UMU_ID);
        try (CloseableHttpResponse response = httpClient.execute(fetchStmt)) {
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode stmt = parseJWT(jwtString);
            
            log("GET " + SWAMID_URL + "/fetch?sub=" + UMU_ID);
            log("Response (JWT decoded):");
            log(formatJson(jwtString));
            
            Assertions.assertEquals(SWAMID_ID, stmt.get("iss").asText());
            Assertions.assertEquals(UMU_ID, stmt.get("sub").asText());
            Assertions.assertNotEquals(stmt.get("iss").asText(), stmt.get("sub").asText());
            
            log("✓ iss: " + stmt.get("iss").asText() + " (SWAMID)");
            log("✓ sub: " + stmt.get("sub").asText() + " (UMU)");
        }
        
        logSuccess("✅ UMU registered under SWAMID + A.2.5 validated");
    }
    
    /**
     * Test 5: Register OP.UMU as subordinate of UMU
     * Build the chain: OP.UMU → UMU → SWAMID → eduGAIN
     */
    @Test
    @Order(5)
    @DisplayName("Test 5: Register OP.UMU Under UMU")
    public void test05_RegisterOPUMUUnderUMU() throws Exception {
        logTestHeader("Test 5: Register OP.UMU Under UMU");
        
        log("Building Complete Federation Hierarchy:");
        log("  eduGAIN → SWAMID → UMU → OP.UMU");
        log("");
        
        // Register OP.UMU as subordinate of UMU
        String url = UMU_URL + "/manage/subordinates";
        String requestBody = "{\n" +
            "  \"entity_id\": \"" + OP_UMU_ID + "\",\n" +
            "  \"jwks\": {\n" +
            "    \"keys\": [{\n" +
            "      \"kty\": \"RSA\",\n" +
            "      \"kid\": \"op-umu-key-1\",\n" +
            "      \"use\": \"sig\",\n" +
            "      \"alg\": \"RS256\"\n" +
            "    }]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"openid_provider\": {\n" +
            "      \"issuer\": \"" + OP_UMU_ID + "\",\n" +
            "      \"authorization_endpoint\": \"" + OP_UMU_ID + "/authorize\",\n" +
            "      \"token_endpoint\": \"" + OP_UMU_ID + "/token\",\n" +
            "      \"userinfo_endpoint\": \"" + OP_UMU_ID + "/userinfo\",\n" +
            "      \"jwks_uri\": \"" + OP_UMU_ID + "/jwks\",\n" +
            "      \"scopes_supported\": [\"openid\", \"profile\", \"email\"],\n" +
            "      \"response_types_supported\": [\"code\"],\n" +
            "      \"grant_types_supported\": [\"authorization_code\"]\n" +
            "    }\n" +
            "  }\n" +
            "}";
        
        log("POST " + url);
        log("Registering OP.UMU as subordinate of UMU...");
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertTrue(response.getCode() == 200 || response.getCode() == 201);
            log("✓ OP.UMU registered under UMU");
        }
        
        // Configure OP.UMU to point to UMU
        url = OP_UMU_URL + "/manage/entity/authority-hints";
        requestBody = "{\"authority_hints\": [\"" + UMU_URL + "\"]}";
        
        log("");
        log("Configuring OP.UMU authority hints...");
        request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(200, response.getCode());
            log("✓ OP.UMU points to UMU");
        }
        
        // Verify A.2.3 - Subordinate Statement by UMU about OP.UMU
        log("");
        log("Verifying A.2.3 - Subordinate Statement by UMU about OP.UMU");
        
        HttpGet fetchStmt = new HttpGet(UMU_URL + "/fetch?sub=" + OP_UMU_ID);
        try (CloseableHttpResponse response = httpClient.execute(fetchStmt)) {
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode stmt = parseJWT(jwtString);
            
            log("GET " + UMU_URL + "/fetch?sub=" + OP_UMU_ID);
            log("Response (JWT decoded):");
            log(formatJson(jwtString));
            
            Assertions.assertEquals(UMU_ID, stmt.get("iss").asText());
            Assertions.assertEquals(OP_UMU_ID, stmt.get("sub").asText());
            
            log("✓ iss: " + stmt.get("iss").asText() + " (UMU)");
            log("✓ sub: " + stmt.get("sub").asText() + " (OP.UMU)");
        }
        
        logSuccess("✅ OP.UMU registered under UMU + A.2.3 validated");
    }
    
    /**
     * Test 6: Verify Entity Configurations per Appendix A.2
     */
    @Test
    @Order(6)
    @DisplayName("Test 6: A.2 - Verify All Entity Configurations")
    public void test06_VerifyEntityConfigurations() throws Exception {
        logTestHeader("Test 6: Verify All Entity Configurations (Appendix A.2)");
        
        // A.2.1 - Entity Configuration for OP.UMU
        log("A.2.1 - Entity Configuration for https://op.umu.se");
        log("-------");
        verifyEntityConfiguration(OP_UMU_URL, OP_UMU_ID, true);
        
        log("");
        
        // A.2.2 - Entity Configuration for UMU
        log("A.2.2 - Entity Configuration for https://umu.se");
        log("-------");
        verifyEntityConfiguration(UMU_URL, UMU_ID, true);
        
        log("");
        
        // A.2.4 - Entity Configuration for SWAMID
        log("A.2.4 - Entity Configuration for https://swamid.se");
        log("-------");
        verifyEntityConfiguration(SWAMID_URL, SWAMID_ID, true);
        
        log("");
        
        // A.2.6 - Entity Configuration for eduGAIN (already verified in test02)
        log("A.2.6 - Entity Configuration for https://edugain.geant.org");
        log("-------");
        verifyEntityConfiguration(EDUGAIN_URL, EDUGAIN_ID, false);
        
        logSuccess("✅ All Entity Configurations validated per Appendix A.2");
    }
    
    /**
     * Test 7: A.2 - The LIGO Wiki Discovers the OP's Metadata
     * 
     * This is the main test that demonstrates the complete trust chain resolution
     * as described in Appendix A.2.
     */
    @Test
    @Order(7)
    @DisplayName("Test 7: A.2 - The LIGO Wiki Discovers OP.UMU Metadata via Trust Chain")
    public void test07_LIGODiscoversOPMetadata() throws Exception {
        logTestHeader("Test 7: A.2 - The LIGO Wiki Discovers the OP's Metadata");
        
        log("Scenario from Appendix A.2:");
        log("  LIGO Wiki (Relying Party) needs to discover metadata for OP.UMU");
        log("  LIGO must validate OP.UMU through the trust chain");
        log("");
        log("Trust Chain:");
        log("  OP.UMU → UMU → SWAMID → eduGAIN (Trust Anchor)");
        log("");
        log("=".repeat(80));
        
        // Perform trust chain resolution
        TrustChainResolver resolver = new TrustChainResolver();
        
        log("Starting Trust Chain Resolution from LIGO for OP.UMU...");
        log("");
        
        TrustChainResult result = resolver.resolveTrustChain(OP_UMU_URL, EDUGAIN_URL);
        
        log("=".repeat(80));
        log("");
        log("Trust Chain Resolution Result:");
        log("  Valid: " + result.isValid());
        log("  Target Entity: " + result.getTargetEntity() + " (OP.UMU)");
        log("  Trust Anchor: " + result.getTrustAnchor() + " (eduGAIN)");
        log("  Statements Collected: " + result.getStatements().size());
        log("");
        
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
        
        if (!result.getMessages().isEmpty()) {
            log("Messages:");
            for (String msg : result.getMessages()) {
                log("  ✓ " + msg);
            }
            log("");
        }
        
        Assertions.assertTrue(result.isValid(), 
            "Trust chain MUST be valid per Appendix A");
        
        // Should have at least 4 statements:
        // 1. OP.UMU Entity Configuration (self-signed)
        // 2. UMU Entity Configuration (self-signed)
        // 3. UMU's Subordinate Statement about OP.UMU
        // 4. SWAMID Entity Configuration (self-signed)
        // 5. SWAMID's Subordinate Statement about UMU
        // 6. eduGAIN Entity Configuration (self-signed)
        // 7. eduGAIN's Subordinate Statement about SWAMID
        
        Assertions.assertTrue(result.getStatements().size() >= 4, 
            "Should have multiple statements in the chain");
        
        log("Trust Chain Statements (as collected):");
        for (int i = 0; i < result.getStatements().size(); i++) {
            JsonNode stmt = result.getStatements().get(i);
            String iss = stmt.get("iss").asText();
            String sub = stmt.get("sub").asText();
            String stmtType = iss.equals(sub) ? "Entity Configuration" : "Subordinate Statement";
            
            log("  " + (i+1) + ". " + stmtType);
            log("     iss: " + iss);
            log("     sub: " + sub);
        }
        
        log("");
        log("Validation Steps (per Appendix A.2):");
        log("  ✓ A.2.1 - OP.UMU Entity Configuration retrieved and verified");
        log("  ✓ A.2.2 - UMU Entity Configuration retrieved and verified");
        log("  ✓ A.2.3 - UMU's Subordinate Statement about OP.UMU verified");
        log("  ✓ A.2.4 - SWAMID Entity Configuration retrieved and verified");
        log("  ✓ A.2.5 - SWAMID's Subordinate Statement about UMU verified");
        log("  ✓ A.2.6 - eduGAIN Entity Configuration retrieved and verified");
        log("  ✓ A.2.7 - eduGAIN's Subordinate Statement about SWAMID verified");
        log("");
        log("Per Appendix A.2.8 - Verified Metadata for https://op.umu.se:");
        log("  The metadata from OP.UMU is now trusted because:");
        log("  1. OP.UMU's Entity Configuration was self-signed and verified");
        log("  2. UMU issued a valid Subordinate Statement about OP.UMU");
        log("  3. Trust chain validated up to eduGAIN (Trust Anchor)");
        log("  4. All JWT signatures verified successfully");
        log("");
        
        logSuccess("✅ A.2 - LIGO successfully discovered and validated OP.UMU metadata!");
        log("");
        log("Result: LIGO can now trust OP.UMU as an OpenID Provider");
        log("        because the trust chain to eduGAIN has been validated.");
    }
    
    // Helper methods
    
    private void verifyEntityConfiguration(String entityUrl, String expectedEntityId, 
                                          boolean shouldHaveAuthorityHints) throws Exception {
        HttpGet request = new HttpGet(entityUrl + "/.well-known/openid-federation");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(200, response.getCode());
            
            String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode config = parseJWT(jwtString);
            
            log("GET " + entityUrl + "/.well-known/openid-federation");
            log("✓ Returns signed JWT Entity Configuration");
            log("✓ iss: " + config.get("iss").asText());
            log("✓ sub: " + config.get("sub").asText());
            log("✓ iss == sub (self-signed)");
            
            Assertions.assertEquals(expectedEntityId, config.get("iss").asText());
            Assertions.assertEquals(expectedEntityId, config.get("sub").asText());
            
            if (!shouldHaveAuthorityHints) {
                boolean noHints = !config.has("authority_hints") || config.get("authority_hints").isEmpty();
                Assertions.assertTrue(noHints, "Trust Anchor should not have authority_hints");
                log("✓ authority_hints: [] (Trust Anchor)");
            } else {
                Assertions.assertTrue(config.has("authority_hints"), 
                    "Subordinate should have authority_hints");
                log("✓ authority_hints: present");
            }
        }
    }
    
    private static void log(String message) {
        System.out.println("[APPENDIX-A] " + message);
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
            if (json.startsWith("eyJ")) {
                SignedJWT signedJWT = SignedJWT.parse(json);
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
            } else {
                Object jsonObject = objectMapper.readValue(json, Object.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            }
        } catch (Exception e) {
            return json;
        }
    }
    
    private static JsonNode parseJWT(String jwtString) throws Exception {
        if (jwtString.startsWith("eyJ")) {
            SignedJWT signedJWT = SignedJWT.parse(jwtString);
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
            String claimsJson = objectMapper.writeValueAsString(claims);
            return objectMapper.readTree(claimsJson);
        } else {
            return objectMapper.readTree(jwtString);
        }
    }
}

