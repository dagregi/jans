package io.jans.federation.service;

import io.jans.federation.model.EntityData;
import io.jans.federation.model.TrustMark;
import com.nimbusds.jose.JOSEException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Trust Mark Service for OpenID Federation 1.0
 * 
 * Handles Trust Mark issuance, storage, and validation.
 * 
 * Trust Marks are signed assertions about an entity's compliance
 * with certain criteria or membership in a particular class.
 * 
 * Reference: OpenID Federation 1.0 Section 5
 */
public class TrustMarkService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrustMarkService.class);
    
    /**
     * Issue a Trust Mark to an entity
     * 
     * @param trustMarkId Trust Mark identifier (e.g., "https://refeds.org/sirtfi")
     * @param subjectEntityId Entity the trust mark is about
     * @param expiresInSeconds Validity period in seconds (null for no expiration)
     * @return Signed Trust Mark JWT
     */
    public static String issueTrustMark(String trustMarkId, String subjectEntityId, Long expiresInSeconds) 
            throws JOSEException {
        EntityData entityData = EntityData.getInstance();
        
        logger.info("Issuing Trust Mark: id={}, subject={}, issuer={}", 
            trustMarkId, subjectEntityId, entityData.getEntityId());
        
        // Create Trust Mark claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", entityData.getEntityId());  // This entity is the issuer
        claims.put("sub", subjectEntityId);            // Subject entity
        claims.put("id", trustMarkId);                 // Trust Mark ID
        claims.put("iat", System.currentTimeMillis() / 1000);
        
        if (expiresInSeconds != null) {
            claims.put("exp", System.currentTimeMillis() / 1000 + expiresInSeconds);
        }
        
        // Sign the Trust Mark
        String signedJWT = JWTService.signEntityStatement(claims);
        
        // Create and store Trust Mark
        TrustMark trustMark = new TrustMark(trustMarkId, entityData.getEntityId(), subjectEntityId);
        trustMark.setIssuedAt(System.currentTimeMillis() / 1000);
        if (expiresInSeconds != null) {
            trustMark.setExpiresAt(System.currentTimeMillis() / 1000 + expiresInSeconds);
        }
        trustMark.setSignedJWT(signedJWT);
        
        entityData.addTrustMark(trustMark);
        
        logger.info("✓ Trust Mark issued and signed");
        logger.debug("  Trust Mark JWT (first 100 chars): {}...", 
            signedJWT.substring(0, Math.min(100, signedJWT.length())));
        
        return signedJWT;
    }
    
    /**
     * Get all Trust Marks issued by this entity
     */
    public static List<TrustMark> getIssuedTrustMarks() {
        return EntityData.getInstance().getTrustMarks();
    }
    
    /**
     * Get a specific Trust Mark by ID
     */
    public static TrustMark getTrustMark(String id) {
        return EntityData.getInstance().getTrustMarkById(id);
    }
    
    /**
     * Revoke a Trust Mark
     */
    public static void revokeTrustMark(String id) {
        EntityData entityData = EntityData.getInstance();
        entityData.removeTrustMark(id);
        logger.info("Trust Mark revoked: {}", id);
    }
}

