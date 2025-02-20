package fun.golinks.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import fun.golinks.gateway.properties.NacosConfig;
import fun.golinks.gateway.properties.SentinelProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

@EnableConfigurationProperties(SentinelProperties.class)
@Configuration
@ImportAutoConfiguration(SentinelConfig.NacosConfiguration.class)
public class SentinelConfig {

    private static final String DATA_TYPE = "data-type";
    private static final String RULE_TYPE = "rule-type";
    private static final String JSON = "json";
    private static final String FLOW = "flow";
    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        // Register the block exception handler for Spring Cloud Gateway.
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @ConditionalOnProperty(prefix = "spring.cloud.sentinel.datasource", name = "type", havingValue = "nacos")
    public static class NacosConfiguration {

        public NacosConfiguration(SentinelProperties sentinelProperties) throws Exception {
            NacosConfig nacosConfig = sentinelProperties.getDatasource().getNacos();
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.getAddress());
            properties.put(PropertyKeyConst.USERNAME, nacosConfig.getUsername());
            properties.put(PropertyKeyConst.PASSWORD, nacosConfig.getPassword());
            properties.put(PropertyKeyConst.NAMESPACE, nacosConfig.getNamespace());
            properties.put(DATA_TYPE, JSON);
            properties.put(RULE_TYPE, FLOW);
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(properties,
                    nacosConfig.getGroupId(), nacosConfig.getDataId(),
                    source -> com.alibaba.fastjson.JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                    }));
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        }
    }

}