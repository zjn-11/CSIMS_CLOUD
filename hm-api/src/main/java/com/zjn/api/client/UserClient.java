package com.zjn.api.client;

import com.zjn.api.config.DefaultFeignConfig;
import com.zjn.api.config.UserInfoConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "user-service",
              configuration = {DefaultFeignConfig.class, UserInfoConfig.class})
public interface UserClient {
    @PutMapping("/users/money/deduct")
     void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);
}
