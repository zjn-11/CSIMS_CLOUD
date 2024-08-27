package com.zjn.api.client;

import com.zjn.api.config.DefaultFeignConfig;
import com.zjn.api.config.UserInfoConfig;
import com.zjn.api.dto.ItemDTO;
import com.zjn.api.dto.OrderDetailDTO;
import com.zjn.api.fallback.ItemClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "item-service", fallbackFactory = ItemClientFallbackFactory.class,
             configuration = {DefaultFeignConfig.class, UserInfoConfig.class})
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);
}
