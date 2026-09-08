package com.dianping.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {
    private final SecretKey secretKey;
    private final JwtProperties properties;

    @Autowired
    public JwtUtils(JwtProperties properties) {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalArgumentException("security.jwt.secret must not be blank");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
        this.properties = properties;
    }

    public JwtUtils(String secretBase64, java.time.Duration ttl) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.properties = new JwtProperties();
        this.properties.setTtl(ttl);
    }

    public String generateToken(String subject, Map<String, ?> claims) {
        Instant now = Instant.now();
        var builder = Jwts.builder().subject(subject);
        if (claims != null) {
            claims.forEach((key, value) -> {
                if (value != null) {
                    builder.claim(key, value);
                }
            });
        }
        return builder.issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getTtl())))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
