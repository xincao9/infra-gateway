package fun.golinks.gateway.properties;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class JwtPropertiesTest {

    @Resource
    private JwtProperties jwtProperties;

    @Test
    public void testParseToken() {
        String subject1 = "xincao";
        try {
            String token = jwtProperties.generatorToken(subject1, Collections.singletonMap("roles", "admin"), TimeUnit.DAYS.toMillis(1));
            System.out.println(token);
            Claims claims = jwtProperties.parseToken(token);
            String subject2 = claims.getSubject();
            Assertions.assertEquals(subject1, subject2);
        } catch (Exception e) {
            log.error("Failed to parse token: ", e);
        }
    }
}
