package io.jans.federation.model;

/**
 * Trust Mark representation per OpenID Federation 1.0
 * 
 * A Trust Mark is a signed statement that an entity meets certain criteria
 * or belongs to a particular class of entities.
 * 
 * Trust Marks are:
 * - Issued by Trust Mark Issuers (often the Trust Anchor)
 * - Included in Entity Configurations
 * - Signed JWTs
 * - Verifiable using issuer's JWKS
 * 
 * Reference: OpenID Federation 1.0 Section 5
 */
public class TrustMark {
    
    private String id;              // Trust Mark identifier (e.g., "https://refeds.org/sirtfi")
    private String issuer;          // Trust Mark issuer entity ID
    private String subject;         // Entity this trust mark is about
    private long issuedAt;          // Unix timestamp when issued
    private Long expiresAt;         // Unix timestamp when expires (optional)
    private String signedJWT;       // The signed Trust Mark JWT
    
    public TrustMark() {
    }
    
    public TrustMark(String id, String issuer, String subject) {
        this.id = id;
        this.issuer = issuer;
        this.subject = subject;
        this.issuedAt = System.currentTimeMillis() / 1000;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public long getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getSignedJWT() {
        return signedJWT;
    }
    
    public void setSignedJWT(String signedJWT) {
        this.signedJWT = signedJWT;
    }
    
    /**
     * Check if trust mark is expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // No expiration
        }
        return System.currentTimeMillis() / 1000 > expiresAt;
    }
    
    @Override
    public String toString() {
        return "TrustMark{" +
                "id='" + id + '\'' +
                ", issuer='" + issuer + '\'' +
                ", subject='" + subject + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", expired=" + isExpired() +
                '}';
    }
}

