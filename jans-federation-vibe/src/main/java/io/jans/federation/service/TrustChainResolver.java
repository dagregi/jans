package io.jans.federation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * Trust Chain Resolver for OpenID Federation 1.0
 * 
 * This class implements trust chain resolution and validation as described in
 * Section 4 of the OpenID Federation 1.0 specification.
 * 
 * Trust Chain Resolution Process:
 * 1. Start with the target entity's Entity Configuration
 * 2. Follow authority_hints to superior entities
 * 3. Fetch Subordinate Statements from each superior
 * 4. Continue until reaching a Trust Anchor
 * 5. Validate the complete chain
 * 
 * Reference: https://openid.net/specs/openid-federation-1_0.html#section-4
 */
public class TrustChainResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(TrustChainResolver.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;
    
    public TrustChainResolver() {
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * Resolve and validate trust chain for a target entity
     * 
     * @param targetEntityUrl Base URL of the target entity
     * @param trustAnchorUrl Base URL of the trust anchor
     * @return TrustChainResult containing the chain and validation status
     */
    public TrustChainResult resolveTrustChain(String targetEntityUrl, String trustAnchorUrl) {
        logger.info("=".repeat(60));
        logger.info("Starting Trust Chain Resolution");
        logger.info("Target Entity: {}", targetEntityUrl);
        logger.info("Trust Anchor: {}", trustAnchorUrl);
        logger.info("=".repeat(60));
        
        TrustChainResult result = new TrustChainResult();
        result.setTargetEntity(targetEntityUrl);
        result.setTrustAnchor(trustAnchorUrl);
        
        try {
            // Step 1: Fetch target entity's Entity Configuration
            logger.info("Step 1: Fetching Entity Configuration from target entity");
            JsonNode targetConfig = fetchEntityConfiguration(targetEntityUrl);
            if (targetConfig == null) {
                result.setValid(false);
                result.addError("Failed to fetch target entity configuration");
                return result;
            }
            
            String targetEntityId = targetConfig.get("iss").asText();
            logger.info("  Target Entity ID: {}", targetEntityId);
            
            List<String> authorityHints = extractAuthorityHints(targetConfig);
            logger.info("  Authority Hints: {}", authorityHints);
            
            result.addStatement(targetConfig);
            
            // If no authority hints, this entity claims to be a Trust Anchor
            if (authorityHints.isEmpty()) {
                logger.info("  No authority hints - entity claims to be Trust Anchor");
                if (targetEntityId.equals(extractEntityIdFromUrl(trustAnchorUrl))) {
                    result.setValid(true);
                    result.addMessage("Entity is the Trust Anchor");
                    return result;
                } else {
                    result.setValid(false);
                    result.addError("Entity has no authority hints but is not the Trust Anchor");
                    return result;
                }
            }
            
            // Step 2: Follow authority hints up the chain
            String currentEntityId = targetEntityId;
            Set<String> visited = new HashSet<>();
            visited.add(currentEntityId);
            
            int hopCount = 0;
            int maxHops = 10; // Prevent infinite loops
            
            while (!authorityHints.isEmpty() && hopCount < maxHops) {
                hopCount++;
                
                // Process the first authority hint (typically there's only one)
                String authorityHint = authorityHints.get(0);
                
                logger.info("");
                logger.info("Step {}: Following authority hint: {}", hopCount + 1, authorityHint);
                
                if (visited.contains(authorityHint)) {
                    result.addError("Circular reference detected: " + authorityHint);
                    break;
                }
                
                // Fetch the authority's Entity Configuration
                logger.info("  Fetching Entity Configuration from authority");
                JsonNode authorityConfig = fetchEntityConfiguration(authorityHint);
                if (authorityConfig == null) {
                    result.addError("Failed to fetch authority configuration: " + authorityHint);
                    break;
                }
                
                String authorityEntityId = authorityConfig.get("iss").asText();
                logger.info("  Authority Entity ID: {}", authorityEntityId);
                
                result.addStatement(authorityConfig);
                visited.add(authorityEntityId);
                
                // Extract authority's JWKS for verifying subordinate statement
                Object jwksObj = authorityConfig.get("jwks");
                if (jwksObj == null) {
                    result.addError("Authority has no JWKS");
                    break;
                }
                
                String jwksJson = objectMapper.writeValueAsString(jwksObj);
                JWKSet authorityJWKS = JWKSet.parse(jwksJson);
                
                // Step 3: Fetch Subordinate Statement about the current entity from this authority
                logger.info("  Fetching Subordinate Statement: sub={} from iss={}", 
                    currentEntityId, authorityEntityId);
                
                JsonNode subordinateStatement = fetchSubordinateStatement(authorityHint, currentEntityId, authorityJWKS);
                if (subordinateStatement == null) {
                    result.addError("Failed to fetch subordinate statement from: " + authorityHint);
                    break;
                }
                
                // Validate the subordinate statement
                String stmtIss = subordinateStatement.get("iss").asText();
                String stmtSub = subordinateStatement.get("sub").asText();
                
                logger.info("  Subordinate Statement: iss={}, sub={}", stmtIss, stmtSub);
                
                if (!stmtIss.equals(authorityEntityId)) {
                    result.addError("Subordinate statement issuer mismatch");
                    break;
                }
                
                if (!stmtSub.equals(currentEntityId)) {
                    result.addError("Subordinate statement subject mismatch");
                    break;
                }
                
                result.addStatement(subordinateStatement);
                
                // Check if we've reached the Trust Anchor
                String trustAnchorEntityId = extractEntityIdFromUrl(trustAnchorUrl);
                if (authorityEntityId.equals(trustAnchorEntityId)) {
                    logger.info("");
                    logger.info("✅ Reached Trust Anchor: {}", trustAnchorEntityId);
                    result.setValid(true);
                    result.addMessage("Trust chain successfully validated");
                    return result;
                }
                
                // Continue up the chain
                currentEntityId = authorityEntityId;
                List<String> nextAuthorityHints = extractAuthorityHints(authorityConfig);
                
                if (nextAuthorityHints.isEmpty()) {
                    // This authority is a Trust Anchor
                    logger.info("  Authority has no further authority hints - it's a Trust Anchor");
                    if (authorityEntityId.equals(trustAnchorEntityId)) {
                        result.setValid(true);
                        result.addMessage("Trust chain validated - reached Trust Anchor");
                        return result;
                    } else {
                        result.setValid(false);
                        result.addError("Reached a Trust Anchor but it's not the expected one");
                        return result;
                    }
                }
                
                // Continue with next level's authority hints
                authorityHints = nextAuthorityHints;
            }
            
            if (hopCount >= maxHops) {
                result.setValid(false);
                result.addError("Maximum hop count exceeded (possible loop)");
            } else {
                result.setValid(false);
                result.addError("Failed to reach Trust Anchor");
            }
            
        } catch (Exception e) {
            logger.error("Error during trust chain resolution", e);
            result.setValid(false);
            result.addError("Exception: " + e.getMessage());
        }
        
        return result;
    }
    
    private JsonNode fetchEntityConfiguration(String entityUrl) {
        String url = entityUrl + "/.well-known/openid-federation";
        logger.info("    GET {}", url);
        
        try {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                logger.info("    Response: {} ({} bytes)", statusCode, jwtString.length());
                
                if (statusCode != 200) {
                    logger.warn("    Failed to fetch entity configuration: HTTP {}", statusCode);
                    return null;
                }
                
                // Parse JWT
                logger.info("    Parsing and verifying JWT signature...");
                SignedJWT signedJWT = SignedJWT.parse(jwtString);
                
                // Extract JWKS from JWT claims for verification
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                Object jwksObj = claims.get("jwks");
                
                if (jwksObj == null) {
                    logger.warn("    No JWKS found in Entity Configuration");
                    return null;
                }
                
                // Convert JWKS to JWKSet
                String jwksJson = objectMapper.writeValueAsString(jwksObj);
                JWKSet jwkSet = JWKSet.parse(jwksJson);
                
                // Verify signature
                boolean verified = JWTService.verifyEntityStatement(jwtString, jwkSet);
                if (!verified) {
                    logger.error("    ✗ JWT signature verification FAILED");
                    return null;
                }
                
                logger.info("    ✓ JWT signature verified successfully");
                
                // Convert claims to JsonNode
                String claimsJson = objectMapper.writeValueAsString(claims);
                return objectMapper.readTree(claimsJson);
            }
        } catch (Exception e) {
            logger.error("    Error fetching/verifying entity configuration", e);
            return null;
        }
    }
    
    private JsonNode fetchSubordinateStatement(String superiorUrl, String subordinateId, JWKSet superiorJWKS) {
        String url = superiorUrl + "/fetch?sub=" + subordinateId;
        logger.info("    GET {}", url);
        
        try {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String jwtString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                logger.info("    Response: {} ({} bytes)", statusCode, jwtString.length());
                
                if (statusCode != 200) {
                    logger.warn("    Failed to fetch subordinate statement: HTTP {}", statusCode);
                    return null;
                }
                
                // Parse JWT
                logger.info("    Parsing and verifying JWT signature...");
                SignedJWT signedJWT = SignedJWT.parse(jwtString);
                
                // Verify signature using superior's JWKS
                boolean verified = JWTService.verifyEntityStatement(jwtString, superiorJWKS);
                if (!verified) {
                    logger.error("    ✗ JWT signature verification FAILED");
                    logger.error("    Subordinate Statement signature could not be verified with superior's JWKS");
                    return null;
                }
                
                logger.info("    ✓ JWT signature verified successfully");
                
                // Extract claims
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                String claimsJson = objectMapper.writeValueAsString(claims);
                return objectMapper.readTree(claimsJson);
            }
        } catch (Exception e) {
            logger.error("    Error fetching/verifying subordinate statement", e);
            return null;
        }
    }
    
    private List<String> extractAuthorityHints(JsonNode config) {
        List<String> hints = new ArrayList<>();
        if (config.has("authority_hints") && config.get("authority_hints").isArray()) {
            for (JsonNode hint : config.get("authority_hints")) {
                hints.add(hint.asText());
            }
        }
        return hints;
    }
    
    private String extractEntityIdFromUrl(String url) {
        // Convert URL to entity ID
        // For Appendix A entities
        if (url.contains("localhost:8080")) {
            return "https://edugain.geant.org";  // eduGAIN
        } else if (url.contains("localhost:8081")) {
            return "https://swamid.se";  // SWAMID
        } else if (url.contains("localhost:8082")) {
            return "https://umu.se";  // UMU
        } else if (url.contains("localhost:8083")) {
            return "https://op.umu.se";  // OP.UMU
        } else if (url.contains("localhost:8084")) {
            return "https://ligo.example.org";  // LIGO
        }
        // For generic nodeN entities
        else if (url.contains("localhost:")) {
            // Extract port and map to nodeN
            try {
                int port = Integer.parseInt(url.substring(url.lastIndexOf(":") + 1));
                int nodeNum = port - 8080 + 1;
                return "https://node" + nodeNum + ".example.com";
            } catch (Exception e) {
                return url;
            }
        }
        return url;
    }
    
    /**
     * Result of trust chain resolution
     */
    public static class TrustChainResult {
        private String targetEntity;
        private String trustAnchor;
        private boolean valid;
        private List<JsonNode> statements = new ArrayList<>();
        private List<String> messages = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public String getTargetEntity() {
            return targetEntity;
        }
        
        public void setTargetEntity(String targetEntity) {
            this.targetEntity = targetEntity;
        }
        
        public String getTrustAnchor() {
            return trustAnchor;
        }
        
        public void setTrustAnchor(String trustAnchor) {
            this.trustAnchor = trustAnchor;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<JsonNode> getStatements() {
            return statements;
        }
        
        public void addStatement(JsonNode statement) {
            this.statements.add(statement);
        }
        
        public List<String> getMessages() {
            return messages;
        }
        
        public void addMessage(String message) {
            this.messages.add(message);
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}

