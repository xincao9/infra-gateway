package fun.golinks.gateway.properties;

import lombok.Data;

/**
 * nacos配置
 */
@Data
public class NacosConfig {

    /**
     * 地址
     */
    private String address;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 组id
     */
    private String groupId;

    /**
     * 数据id
     */
    private String dataId;
}
