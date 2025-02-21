package fun.golinks.gateway.properties;

import lombok.Data;

/**
 * 数据源
 */
@Data
public class DataSourceProperties {

    /**
     * 类型
     */
    private String type = "nacos";

    /**
     * nacos配置
     */
    private NacosProperties nacos = new NacosProperties();
}