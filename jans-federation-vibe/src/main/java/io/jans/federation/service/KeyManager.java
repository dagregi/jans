package io.jans.federation.service;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Key Manager for Federation Entity
 * 
 * Generates and manages RSA key pairs for:
 * - Signing Entity Statements (JWT)
 * - Publishing public keys in JWKS
 * 
 * Each entity generates its own key pair at startup.
 * Private key is kept in memory only.
 * Public key is published in JWKS.
 */
public class KeyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);
    private static KeyManager instance;
    
    private KeyPair keyPair;
    private String keyId;
    private RSAKey rsaJWK;
    
    private KeyManager() {
        // Private constructor for singleton
    }
    
    public static synchronized KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }
    
    /**
     * Initialize keys for the entity
     * 
     * @param entityName Name of the entity (e.g., "node1")
     */
    public void initialize(String entityName) throws NoSuchAlgorithmException {
        logger.info("Generating RSA key pair for entity: {}", entityName);
        
        // Generate RSA key pair (2048 bits)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.keyPair = keyGen.generateKeyPair();
        
        this.keyId = entityName + "-key-1";
        
        // Create JWK from key pair
        this.rsaJWK = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(keyId)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .build();
        
        logger.info("✓ RSA key pair generated successfully");
        logger.info("  Key ID: {}", keyId);
        logger.info("  Algorithm: RS256");
        logger.info("  Key Size: 2048 bits");
        logger.info("  Public Key (first 50 chars): {}...", 
            Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).substring(0, 50));
    }
    
    /**
     * Get the private key for signing
     */
    public PrivateKey getPrivateKey() {
        if (keyPair == null) {
            throw new IllegalStateException("KeyManager not initialized. Call initialize() first.");
        }
        return keyPair.getPrivate();
    }
    
    /**
     * Get the public key
     */
    public PublicKey getPublicKey() {
        if (keyPair == null) {
            throw new IllegalStateException("KeyManager not initialized. Call initialize() first.");
        }
        return keyPair.getPublic();
    }
    
    /**
     * Get the key ID
     */
    public String getKeyId() {
        return keyId;
    }
    
    /**
     * Get the RSA JWK (for publishing in JWKS)
     */
    public RSAKey getRSAJWK() {
        return rsaJWK;
    }
    
    /**
     * Get JWKS representation (public key only)
     */
    public RSAKey getPublicJWK() {
        if (rsaJWK == null) {
            throw new IllegalStateException("KeyManager not initialized");
        }
        return rsaJWK.toPublicJWK();
    }
    
    /**
     * Reset the instance (for testing)
     */
    public static synchronized void reset() {
        instance = null;
    }
}

