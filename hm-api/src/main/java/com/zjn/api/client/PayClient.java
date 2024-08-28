package com.zjn.api.client;


import com.zjn.api.config.DefaultFeignConfig;
import com.zjn.api.config.UserInfoConfig;
import com.zjn.api.dto.PayOrderDTO;
import com.zjn.api.fallback.PayClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "pay-service",
        configuration = {DefaultFeignConfig.class, UserInfoConfig.class},
        fallbackFactory = PayClientFallbackFactory.class)
public interface PayClient {
    @GetMapping("/pay-orders/biz/{id}")
    PayOrderDTO queryPayOrderByBizNo(@PathVariable("id") Long id);
}
