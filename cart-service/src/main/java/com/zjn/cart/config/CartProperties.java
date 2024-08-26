package com.zjn.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("hm.cart")
@Data
@Component
public class CartProperties {

    // 购物车最大数量
    private Integer maxItems;
}
