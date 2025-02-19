package fun.golinks.gateway.properties;

import lombok.Data;

/**
 * 数据源
 */
@Data
public class DataSource {

    /**
     * 类型
     */
    private String type = "nacos";

    /**
     * nacos配置
     */
    private NacosConfig nacos = new NacosConfig();
}