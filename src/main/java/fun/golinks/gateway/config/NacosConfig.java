package fun.golinks.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Slf4j
@EnableDiscoveryClient
@Configuration
public class NacosConfig implements InitializingBean {

    private static final String DATA_ID = "gateway-routes.yaml";
    private static final String GROUP = "infra-gateway";
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NacosConfigManager nacosConfigManager;
    public NacosConfig(RouteDefinitionWriter routeDefinitionWriter,
                       ApplicationEventPublisher applicationEventPublisher,
                       NacosConfigManager nacosConfigManager) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.applicationEventPublisher = applicationEventPublisher;
        this.nacosConfigManager = nacosConfigManager;
    }

    private void updateRoutes() {
        try {
            String config = nacosConfigManager.getConfigService().getConfig(DATA_ID, GROUP, 5000);
            List<RouteDefinition> routes = JSON.parseArray(config, RouteDefinition.class);
            if (routeDefinitionWriter instanceof RouteDefinitionLocator) {
                RouteDefinitionLocator routeDefinitionLocator = (RouteDefinitionLocator) routeDefinitionWriter;
                routeDefinitionLocator.getRouteDefinitions().subscribe(new Consumer<RouteDefinition>() {
                    @Override
                    public void accept(RouteDefinition routeDefinition) {
                        routeDefinitionWriter.delete(Mono.just(routeDefinition.getId())).subscribe();
                    }
                });
            }
            // 添加新路由
            for (RouteDefinition route : routes) {
                routeDefinitionWriter.save(Mono.just(route)).subscribe();
            }
            // 刷新路由
            applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
        } catch (Exception e) {
            log.error("Failed to update routes from Nacos", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化时加载路由
        updateRoutes();
        // 监听 Nacos 配置变化
        nacosConfigManager.getConfigService().addListener(DATA_ID, GROUP, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                updateRoutes();
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        });
    }
}
