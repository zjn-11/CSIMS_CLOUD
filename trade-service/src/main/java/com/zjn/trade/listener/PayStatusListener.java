package com.zjn.trade.listener;

import com.zjn.trade.domain.po.Order;
import com.zjn.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Objects;

/*
* RabbitMQ 交易模块接收消息
* */
@Component
@RequiredArgsConstructor
public class PayStatusListener {

    private final IOrderService orderService;


    /*如果支付成功就修改为已支付状态*/
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue",
                    durable = "true",
                    arguments = @Argument(name = "x-queue-mode", value = "lazy")),
            exchange = @Exchange(name = "pay.direct", type = ExchangeTypes.DIRECT),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId) {
        /*保证消费幂等性*/
        // 1. 查询订单
        Order order = orderService.getById(orderId);
        // 2. 如果为空或者状态不是未支付，就直接不处理
        if (Objects.isNull(order) || order.getStatus() != 1)
            return;
        // 3. 标记订单状态为已支付
        orderService.markOrderPaySuccess(orderId);
    }
}
