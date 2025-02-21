package fun.golinks.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * sentinel配置
 */
@Data
@ConfigurationProperties(prefix = "spring.cloud.sentinel")
public class SentinelProperties {

    /**
     * 数据源
     */
    private DataSourceProperties datasource = new DataSourceProperties();
}
