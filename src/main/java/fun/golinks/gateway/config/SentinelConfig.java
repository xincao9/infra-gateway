package fun.golinks.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import fun.golinks.gateway.properties.NacosProperties;
import fun.golinks.gateway.properties.SentinelProperties;
import fun.golinks.gateway.sentinel.GatewayBlockRequestHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.*;

@Configuration
@ImportAutoConfiguration(SentinelConfig.NacosConfiguration.class)
public class SentinelConfig implements InitializingBean {

    private static final String DATA_TYPE = "data-type";
    private static final String RULE_TYPE = "rule-type";
    private static final String JSON_TYPE = "json";
    private static final String FLOW_TYPE = "flow";
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

    @Bean
    public GatewayBlockRequestHandler gatewayBlockRequestHandler() {
        GatewayBlockRequestHandler gatewayBlockRequestHandler = new GatewayBlockRequestHandler();
        GatewayCallbackManager.setBlockHandler(gatewayBlockRequestHandler);
        return gatewayBlockRequestHandler;
    }

    @Override
    public void afterPropertiesSet() {
        /*
         * 初始化自定义的API分组
         */
        try {
            Set<ApiDefinition> definitions = Collections.synchronizedSet(new HashSet<>());
            ApiDefinition sampleApi = new ApiDefinition("sample-api")
                    .setPredicateItems(Collections.singleton(new ApiPathPredicateItem().setPattern("/sample/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)));
            definitions.add(sampleApi);
            GatewayApiDefinitionManager.loadApiDefinitions(definitions);
        } catch (Exception e) {
            // 处理异常，例如记录日志或抛出运行时异常
            throw new RuntimeException("Failed to load API definitions", e);
        }
    }

    @ConditionalOnProperty(prefix = "spring.cloud.sentinel.datasource", name = "type", havingValue = "nacos")
    public static class NacosConfiguration {

        public NacosConfiguration(SentinelProperties sentinelProperties) {
            NacosProperties nacosProperties = sentinelProperties.getDatasource().getNacos();
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, nacosProperties.getAddress());
            properties.put(PropertyKeyConst.USERNAME, nacosProperties.getUsername());
            properties.put(PropertyKeyConst.PASSWORD, nacosProperties.getPassword());
            properties.put(PropertyKeyConst.NAMESPACE, nacosProperties.getNamespace());
            properties.put(DATA_TYPE, JSON_TYPE);
            properties.put(RULE_TYPE, FLOW_TYPE);
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(properties,
                    nacosProperties.getGroupId(), nacosProperties.getDataId(),
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                    }));
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        }
    }

}