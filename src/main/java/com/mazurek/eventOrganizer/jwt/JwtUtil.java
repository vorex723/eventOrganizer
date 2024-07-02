package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Component

public class JwtUtil {

    private final long jwtTimeValidity;
    private final String secret;

    private final UserRepository userRepository;

    public JwtUtil(@Value("${jwt.expirationTime}") long jwtTimeValidity, @Value("${jwt.secret}") String secret, UserRepository userRepository) {
        this.jwtTimeValidity = jwtTimeValidity;
        this.secret = secret;
        this.userRepository = userRepository;
    }

    public String extractUsername(String token){
        try{
            return extractClaim(token, Claims::getSubject);
        }
        catch (MalformedJwtException exception){
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
       return Jwts
               .builder()
               .setClaims(extraClaims)
               .setSubject(userDetails.getUsername())
               .setIssuedAt(new Date(Calendar.getInstance().getTimeInMillis()+2000))
               .setExpiration(new Date(Calendar.getInstance().getTimeInMillis()+jwtTimeValidity))
               .signWith(getSignInKey(), SignatureAlgorithm.HS256)
               .compact();

    }

    public boolean isTokenValid(String token, UserDetails userDetails) throws ExpiredJwtException{
        final String username = extractUsername(token);
        final Long issuanceDate = extractIssuanceDate(token).getTime();

        Optional<User> userOptional = userRepository.findByEmail(userDetails.getUsername());
        if (userRepository.findByEmail(username).isEmpty())
            throw new UsernameNotFoundException("There is no user with that email.");
        if(userOptional.isEmpty())
            return false;
        return (username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && (userOptional.get().getLastCredentialsChangeTime() <= issuanceDate));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractIssuanceDate(String token){
        return extractClaim(token, Claims::getIssuedAt);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token){
        Claims claims = new DefaultClaims();
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        } catch (ExpiredJwtException e){
            System.out.println("Token has expired");
        }
        return claims;
    }

    private Key getSignInKey() {
        byte [] keyBytes= Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
