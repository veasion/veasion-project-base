package cn.veasion.project.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    private static Key SIGNING_KEY;

    public static String createToken(String id, String name, long ttlMillis) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", id);
        return Jwts.builder()
                .signWith(getSigningKey())
                .setClaims(map)
                .setSubject(name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .compact();
    }

    public static Claims parseJWT(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    public static boolean isExpired(Claims claims) {
        try {
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public static String getUserId(Claims claims) {
        return (String) claims.get("userId");
    }

    public static String getName(Claims claims) {
        return claims.getSubject();
    }

    private static Key getSigningKey() {
        if (SIGNING_KEY != null) {
            return SIGNING_KEY;
        }
        synchronized (JwtUtils.class) {
            if (SIGNING_KEY != null) {
                return SIGNING_KEY;
            }
            String jwtSecret = SpringBeanUtils.getProperties("jwtSecret", String.class);
            if (!StringUtils.hasText(jwtSecret)) {
                throw new RuntimeException("jwtSecret未设置");
            }
            SIGNING_KEY = new SecretKeySpec(DatatypeConverter.parseBase64Binary(jwtSecret), SignatureAlgorithm.HS256.getJcaName());
        }
        return SIGNING_KEY;
    }

}
