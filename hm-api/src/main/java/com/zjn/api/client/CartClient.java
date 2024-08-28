package com.zjn.api.client;

import com.zjn.api.config.DefaultFeignConfig;
import com.zjn.api.config.UserInfoConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient(value = "cart-service",
        configuration = {DefaultFeignConfig.class, UserInfoConfig.class})
public interface CartClient {
    @DeleteMapping("/carts")
    void removeByItemIds(@RequestParam("ids") Collection<Long> ids);
}
