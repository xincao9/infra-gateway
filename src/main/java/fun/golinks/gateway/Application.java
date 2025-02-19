package fun.golinks.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        setUp();
        SpringApplication.run(Application.class, args);
    }

    public static void setUp() {
        // TODO
        System.setProperty("csp.sentinel.dashboard.server", "");
        System.setProperty("csp.sentinel.api.port", "8719");
        System.setProperty("csp.sentinel.app.type", "1");
    }
}
