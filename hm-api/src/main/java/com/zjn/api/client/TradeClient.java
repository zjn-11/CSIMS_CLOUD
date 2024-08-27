package com.zjn.api.client;

import com.zjn.api.config.DefaultFeignConfig;
import com.zjn.api.config.UserInfoConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;


@FeignClient(value = "trade-service",
             configuration = {DefaultFeignConfig.class, UserInfoConfig.class})
public interface TradeClient {

    @PutMapping("/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
}
