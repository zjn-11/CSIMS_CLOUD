package com.zjn.api.fallback;

import com.hmall.common.utils.CollUtils;
import com.zjn.api.client.ItemClient;
import com.zjn.api.dto.ItemDTO;
import com.zjn.api.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

/*
* 实现远程调用item-service失败的fallback逻辑，提供友好提示
* */
@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("查询商品失败：", cause);
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("扣减商品库存失败：", cause);
                throw new RuntimeException(cause);
            }
        };
    }
}
