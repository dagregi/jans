package io.jans.federation.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * JWT Service for signing and verifying Entity Statements
 * 
 * This service uses Nimbus JOSE JWT library to:
 * - Sign Entity Statements with entity's private key
 * - Verify signatures using public keys from JWKS
 * 
 * All Entity Statements MUST be signed JWTs per OpenID Federation 1.0 spec.
 */
public class JWTService {
    
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);
    
    /**
     * Sign an Entity Statement
     * 
     * @param claims Map of JWT claims
     * @return Signed JWT string
     */
    public static String signEntityStatement(Map<String, Object> claims) throws JOSEException {
        KeyManager keyManager = KeyManager.getInstance();
        
        logger.debug("Signing Entity Statement with key: {}", keyManager.getKeyId());
        
        // Build JWT claims
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            claimsBuilder.claim(entry.getKey(), entry.getValue());
        }
        
        JWTClaimsSet claimsSet = claimsBuilder.build();
        
        // Create JWT header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keyManager.getKeyId())
            .type(JOSEObjectType.JWT)
            .build();
        
        // Create signed JWT
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        
        // Sign with private key
        JWSSigner signer = new RSASSASigner(keyManager.getRSAJWK());
        signedJWT.sign(signer);
        
        String jwt = signedJWT.serialize();
        
        logger.debug("✓ Entity Statement signed successfully");
        logger.debug("  JWT (first 100 chars): {}...", jwt.substring(0, Math.min(100, jwt.length())));
        
        return jwt;
    }
    
    /**
     * Verify a signed Entity Statement
     * 
     * @param jwt Signed JWT string
     * @param jwks JWKS containing public key
     * @return true if signature is valid
     */
    public static boolean verifyEntityStatement(String jwt, JWKSet jwks) {
        try {
            // Parse the signed JWT
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            
            // Get the key ID from JWT header
            String kid = signedJWT.getHeader().getKeyID();
            logger.debug("Verifying JWT signed with key ID: {}", kid);
            
            // Find the key in JWKS
            JWK jwk = jwks.getKeyByKeyId(kid);
            if (jwk == null) {
                logger.warn("Key with ID {} not found in JWKS", kid);
                return false;
            }
            
            // Verify it's an RSA key
            if (!(jwk instanceof RSAKey)) {
                logger.warn("Key {} is not an RSA key", kid);
                return false;
            }
            
            RSAKey rsaKey = (RSAKey) jwk;
            
            // Create verifier with public key
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) rsaKey.toPublicKey());
            
            // Verify signature
            boolean verified = signedJWT.verify(verifier);
            
            if (verified) {
                logger.debug("✓ JWT signature verified successfully");
            } else {
                logger.warn("✗ JWT signature verification failed");
            }
            
            return verified;
            
        } catch (ParseException e) {
            logger.error("Failed to parse JWT", e);
            return false;
        } catch (JOSEException e) {
            logger.error("Failed to verify JWT signature", e);
            return false;
        }
    }
    
    /**
     * Parse and extract claims from a JWT (without verification)
     * Used for reading claims before verification
     * 
     * @param jwt Signed JWT string
     * @return JWT claims as map
     */
    public static Map<String, Object> parseJWTClaims(String jwt) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        return signedJWT.getJWTClaimsSet().getClaims();
    }
    
    /**
     * Verify JWT and extract claims
     * 
     * @param jwt Signed JWT string
     * @param jwks JWKS for verification
     * @return Claims if valid, null if invalid
     */
    public static Map<String, Object> verifyAndExtractClaims(String jwt, JWKSet jwks) {
        try {
            if (!verifyEntityStatement(jwt, jwks)) {
                logger.warn("JWT signature verification failed");
                return null;
            }
            
            return parseJWTClaims(jwt);
        } catch (Exception e) {
            logger.error("Failed to verify and extract JWT claims", e);
            return null;
        }
    }
}

