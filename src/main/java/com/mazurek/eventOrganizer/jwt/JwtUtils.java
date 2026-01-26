package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.Principal;
import java.util.*;
import java.util.function.Function;

@Component

public class JwtUtils {

    private final Long accessTokenExpiration;
    private final String secret;

    public JwtUtils(@Value("${jwt.access.expiration:1800000}") Long accessTokenExpiration,
                    @Value("${jwt.secret}") String secret) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.secret = secret;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream()
                .map(Role::getName)
                .toList());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }
    public UUID extractUserId(String token){
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }
    public Collection<? extends GrantedAuthority> extractAuthorities(String token){
        List<String> roles = extractClaim(token, claims -> claims.get("roles", List.class));
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token){
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e){
            return false;
        }
    }

    private boolean isTokenExpired(String token){
        return new Date().after(extractExpiration(token));
    }
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    public Long getAccessTokenExpiration(){
        return accessTokenExpiration;
    }

    private Key getSignInKey() {
        byte [] keyBytes= Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
