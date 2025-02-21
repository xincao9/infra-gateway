package fun.golinks.gateway.properties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

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

    /**
     * 生成一个紧凑的JWT（JSON Web Token）字符串。
     * <p>
     * 该函数使用提供的主题（subject）和角色（roles）信息构建一个JWT，并使用预定义的密钥进行签名。
     * 最终返回一个紧凑的、经过签名的JWT字符串。
     *
     * @param subject JWT的主题，通常表示用户或实体的唯一标识。
     * @param roles   JWT中包含的角色信息，通常用于权限控制。
     * @return 返回一个紧凑的、经过签名的JWT字符串。
     */
    public String generatorToken(String subject, String roles) {
        // 使用Jwts.builder()构建JWT，设置主题和角色信息，并使用密钥进行签名，最后生成紧凑的JWT字符串。
        return Jwts.builder().subject(subject).claim("roles", roles).signWith(key).compact();
    }

    public static void main(String... args) {
        JwtProperties jwtProperties = new JwtProperties();
        String token = jwtProperties.generatorToken("xincao", "admin");
        System.out.println(token);
        System.out.println(jwtProperties.parseToken(token));
    }

}
