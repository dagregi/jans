package io.jans.federation.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * REST endpoint for OpenID Federation 1.0 operations
 */
@Path("/federation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FederationEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(FederationEndpoint.class);
    
    /**
     * Get federation metadata (Section 3.2)
     */
    @GET
    @Path("/metadata")
    public Response getFederationMetadata() {
        logger.info("Getting federation metadata");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("federation_name", "Example Federation");
        metadata.put("version", "1.0");
        metadata.put("contact", "admin@federation.example.com");
        metadata.put("issuer", "https://federation.example.com");
        metadata.put("jwks_uri", "https://federation.example.com/jwks");
        metadata.put("authority_hints", Arrays.asList("https://authority.example.com"));
        
        return Response.ok(metadata).build();
    }
    
    /**
     * Validate trust chain (Section 4)
     */
    @POST
    @Path("/validate-trust-chain")
    public Response validateTrustChain(Map<String, Object> request) {
        logger.info("Validating trust chain");
        
        String entityId = (String) request.get("entity_id");
        String trustMarkId = (String) request.get("trust_mark_id");
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("entity_id", entityId);
        result.put("trust_mark_id", trustMarkId);
        result.put("trust_chain", Arrays.asList(
            "https://federation.example.com",
            "https://authority.example.com",
            entityId
        ));
        
        return Response.ok(result).build();
    }
    
    /**
     * Get trust marks (Section 3.4)
     */
    @GET
    @Path("/trust-marks")
    public Response getTrustMarks(@QueryParam("entity_id") String entityId) {
        logger.info("Getting trust marks");
        
        List<Map<String, Object>> trustMarks = new ArrayList<>();
        
        // Sample trust marks
        trustMarks.add(createMockTrustMark("https://op.example.com", "basic-trust"));
        trustMarks.add(createMockTrustMark("https://rp.example.com", "basic-trust"));
        trustMarks.add(createMockTrustMark("https://test-op.example.com", "advanced-trust"));
        
        return Response.ok(trustMarks).build();
    }
    
    /**
     * Issue trust mark
     */
    @POST
    @Path("/issue-trust-mark")
    public Response issueTrustMark(Map<String, Object> request) {
        logger.info("Issuing trust mark");
        
        String entityId = (String) request.get("entity_id");
        String trustMarkId = (String) request.get("trust_mark_id");
        
        Map<String, Object> result = createMockTrustMark(entityId, trustMarkId);
        result.put("status", "issued");
        
        return Response.ok(result).build();
    }
    
    /**
     * Get trust mark issuers (Section 3.3)
     */
    @GET
    @Path("/trust-mark-issuers")
    public Response getTrustMarkIssuers() {
        logger.info("Getting trust mark issuers");
        
        List<Map<String, Object>> issuers = new ArrayList<>();
        
        Map<String, Object> issuer1 = new HashMap<>();
        issuer1.put("issuer", "https://trustmark.example.com");
        issuer1.put("subject", "https://trustmark.example.com");
        issuer1.put("trust_mark_issuers", Arrays.asList("https://trustmark.example.com"));
        issuers.add(issuer1);
        
        Map<String, Object> issuer2 = new HashMap<>();
        issuer2.put("issuer", "https://authority.example.com");
        issuer2.put("subject", "https://authority.example.com");
        issuer2.put("trust_mark_issuers", Arrays.asList("https://authority.example.com"));
        issuers.add(issuer2);
        
        return Response.ok(issuers).build();
    }
    
    /**
     * Get JWKS (Section 6)
     */
    @GET
    @Path("/jwks")
    public Response getJwks() {
        logger.info("Getting JWKS");
        
        Map<String, Object> jwks = new HashMap<>();
        List<Map<String, Object>> keys = new ArrayList<>();
        
        Map<String, Object> key = new HashMap<>();
        key.put("kty", "RSA");
        key.put("kid", "federation-key-1");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("n", "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw");
        key.put("e", "AQAB");
        keys.add(key);
        
        jwks.put("keys", keys);
        
        return Response.ok(jwks).build();
    }
    
    // Helper methods
    
    private Map<String, Object> createMockEntityConfiguration(String issuer) {
        Map<String, Object> config = new HashMap<>();
        config.put("iss", issuer);
        config.put("sub", issuer);
        config.put("aud", "federation");
        config.put("exp", System.currentTimeMillis() / 1000 + 31536000); // 1 year from now
        config.put("iat", System.currentTimeMillis() / 1000);
        config.put("jti", UUID.randomUUID().toString());
        config.put("authority_hints", Arrays.asList("https://authority.example.com"));
        
        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", Arrays.asList(
            Map.of("kty", "RSA", "kid", "key-1", "use", "sig", "alg", "RS256")
        ));
        config.put("jwks", jwks);
        
        Map<String, Object> metadata = new HashMap<>();
        if (issuer.contains("op")) {
            Map<String, Object> opMetadata = new HashMap<>();
            opMetadata.put("issuer", issuer);
            opMetadata.put("authorization_endpoint", issuer + "/auth");
            opMetadata.put("token_endpoint", issuer + "/token");
            opMetadata.put("userinfo_endpoint", issuer + "/userinfo");
            opMetadata.put("jwks_uri", issuer + "/jwks");
            metadata.put("openid_provider", opMetadata);
        } else if (issuer.contains("rp")) {
            Map<String, Object> rpMetadata = new HashMap<>();
            rpMetadata.put("client_id", "rp-client-1");
            rpMetadata.put("redirect_uris", Arrays.asList(issuer + "/callback"));
            rpMetadata.put("response_types", Arrays.asList("code"));
            rpMetadata.put("grant_types", Arrays.asList("authorization_code"));
            metadata.put("openid_relying_party", rpMetadata);
        }
        config.put("metadata", metadata);
        
        return config;
    }
    
    private Map<String, Object> createMockTrustMark(String entityId, String trustMarkId) {
        Map<String, Object> trustMark = new HashMap<>();
        trustMark.put("entity_id", entityId);
        trustMark.put("issuer", "https://trustmark.example.com");
        trustMark.put("subject", entityId);
        trustMark.put("trust_mark_id", trustMarkId);
        trustMark.put("trust_mark", "https://trustmark.example.com/trustmarks/" + trustMarkId);
        trustMark.put("issued_at", System.currentTimeMillis() / 1000);
        trustMark.put("expiration_time", System.currentTimeMillis() / 1000 + 31536000);
        
        Map<String, Object> mark = new HashMap<>();
        mark.put("level", trustMarkId.replace("-trust", ""));
        mark.put("issued_by", "https://trustmark.example.com");
        trustMark.put("mark", mark);
        
        return trustMark;
    }
}
