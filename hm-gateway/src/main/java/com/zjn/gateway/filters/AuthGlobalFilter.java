package com.zjn.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.CollUtils;
import com.zjn.gateway.config.AuthProperties;
import com.zjn.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取request
        ServerHttpRequest request = exchange.getRequest();
        // 2. 判断是否需要拦截
        if (isExclude(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        // 3. 获取token
        String token = null;
        List<String> authorization = request.getHeaders().get("authorization");
        if (!CollUtils.isEmpty(authorization) && authorization.size() > 0) {
            token = authorization.get(0);
        }
        // 4. 解析并校验token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        // 5. 传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();

        return chain.filter(swe);
    }

    private boolean isExclude(String path) {
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path))
                return true;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
