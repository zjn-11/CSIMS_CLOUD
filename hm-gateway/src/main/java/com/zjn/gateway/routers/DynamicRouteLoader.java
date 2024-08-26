package com.zjn.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/*
* 实现动态路由
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final NacosConfigManager manager; // 用于获取nacos连接
    private final RouteDefinitionWriter writer; // 用于更新网关路由配置
    private final Set<String> routeIds = new HashSet<>(); // 保存所有的路由id
    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";

    @PostConstruct
    public void initRouteConfigLoader() throws NacosException {
        ConfigService configService = manager.getConfigService();
        // 1. 项目启动时，先拉去一起配置，并添加配置更新监听器
        // configInfo：获取到的对应配置文件中的配置信息
        String configInfo = configService.getConfigAndSignListener(
                dataId, group, 5000,
                new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        // 2. 监听到配置变更，需要更新路由表
                        updateConfigInfo(configInfo);
                    }
                });
        // 3. 第一次读取到配置也需要更新到路由表
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo) {
        log.debug("监听到的路由配置信息：{}", configInfo);
        // 1. 解析配置文件解析为RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2. 删除旧的路由表
        Iterator<String> iterator = routeIds.iterator();
        while (iterator.hasNext()) {
            writer.delete(Mono.just(iterator.next())).subscribe();
            iterator.remove();
        }
        // 3. 遍历所有的路由
        routeDefinitions.forEach(routeDefinition -> {
            // 3.1 更新路由表的具体操作
            writer.save(Mono.just(routeDefinition)).subscribe();
            // 3.2 记录路由id，方便下一次删除
            routeIds.add(routeDefinition.getId());
        });
    }

}
