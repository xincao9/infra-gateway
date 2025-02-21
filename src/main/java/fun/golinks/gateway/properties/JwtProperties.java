package fun.golinks.gateway.properties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey = "this-is-a-very-long-secret-key-for-hs512-algorithm-to-ensure-64-bytes-length12345"; // 至少 256 位密钥
    private SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }


    public Claims parseToken(String token) {
        log.info("Secret key length: {} bytes", secretKey.getBytes().length);
        if (secretKey.getBytes().length < 64) {
            log.error("Secret key is too short for HS512, required: 64 bytes, actual: {} bytes", secretKey.getBytes().length);
        }
        try {
            return Jwts.parser()
                    .decryptWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Throwable e) {
            log.error("Invalid JWT token", e);
            return null;
        }
    }

    public boolean isInvalid(String token) {
        return parseToken(token) == null;
    }

    public String generatorToken(String subject, Map<String, Object> claims, long expirationMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        // 过期时间
        Date expiryDate = new Date(nowMillis + expirationMillis);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)              // 设置 subject（如用户 ID）
                .issuedAt(now)                 // 签发时间
                .expiration(expiryDate)        // 过期时间
                .signWith(key).compact();      // 使用 HS512 签名
    }

    public static void main(String... args) {
        JwtProperties jwtProperties = new JwtProperties();
        String token = jwtProperties.generatorToken("xincao", Collections.singletonMap("roles", "admin"), TimeUnit.DAYS.toMillis(1));
        System.out.println(token);
        System.out.println(jwtProperties.parseToken(token));
    }

}
