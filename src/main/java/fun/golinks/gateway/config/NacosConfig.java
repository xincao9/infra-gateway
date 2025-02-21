package fun.golinks.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import fun.golinks.gateway.util.JsonUtil;
import fun.golinks.gateway.util.YamlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@EnableDiscoveryClient
@Configuration
public class NacosConfig implements InitializingBean {

    private static final String DATA_ID = "gateway-routes.yaml";
    private static final String GROUP = "infra-gateway";
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NacosConfigManager nacosConfigManager;
    private final ExecutorService refreshRoutesEventExecutorService = Executors.newSingleThreadExecutor();

    public NacosConfig(RouteDefinitionWriter routeDefinitionWriter, ApplicationEventPublisher applicationEventPublisher,
                       NacosConfigManager nacosConfigManager) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.applicationEventPublisher = applicationEventPublisher;
        this.nacosConfigManager = nacosConfigManager;
    }

    private void updateRoutes() {
        try {
            String config = nacosConfigManager.getConfigService().getConfig(DATA_ID, GROUP, 5000);
            if (StringUtils.isBlank(config)) {
                log.warn("Config from Nacos is empty, skipping route update.");
                return;
            }
            List<RouteDefinition> routes = null;
            if (JsonUtil.isJson(config)) {
                routes = JsonUtil.toArray(config, RouteDefinition.class);
            } else {
                routes = YamlUtil.toArray(config, RouteDefinition.class);
            }
            if (routes == null) {
                return;
            }
            if (routeDefinitionWriter instanceof RouteDefinitionLocator) {
                RouteDefinitionLocator routeDefinitionLocator = (RouteDefinitionLocator) routeDefinitionWriter;
                routeDefinitionLocator.getRouteDefinitions()
                        .flatMap(routeDefinition -> routeDefinitionWriter.delete(Mono.just(routeDefinition.getId())))
                        .thenMany(Flux.fromIterable(routes)
                                .flatMap(route -> routeDefinitionWriter.save(Mono.just(route))))
                        .subscribe(null, error -> log.error("Failed to update routes", error), () -> {
                            refreshRoutesEventExecutorService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
                                    log.info("Routes updated successfully.");
                                }
                            });
                        });
            } else {
                Flux.fromIterable(routes).flatMap(route -> routeDefinitionWriter.save(Mono.just(route))).subscribe(null,
                        error -> log.error("Failed to add new routes", error), () -> {
                            refreshRoutesEventExecutorService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
                                    log.info("New routes added successfully.");
                                }
                            });
                        });
            }
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
                // 提供一个合适的 Executor，例如使用线程池
                return Executors.newSingleThreadExecutor();
            }
        });
    }

}
