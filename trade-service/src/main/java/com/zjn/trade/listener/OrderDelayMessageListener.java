package com.zjn.trade.listener;


import cn.hutool.core.util.ObjectUtil;
import com.zjn.api.client.PayClient;
import com.zjn.api.dto.PayOrderDTO;
import com.zjn.trade.constants.MQConstants;
import com.zjn.trade.domain.po.Order;
import com.zjn.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final IOrderService orderService;
    private final PayClient payClient;

    /**
     * 订单状态兜底方案（以防支付成功消息没有被正确处理）
     * 如果订单超时，由此方法恢复库存
     * @param orderId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MQConstants.DELAY_ORDER_KEY
    ))
    public void listenOrderDelayMessage(Long orderId) {
        // 1. 检查订单状态
        Order order = orderService.getById(orderId);
        // 1.1 订单不存在或者已支付，就不做处理
        if (ObjectUtil.isEmpty(order) && order.getStatus() != 1)
            return;
        // 2. 如果未支付，查询支付流水（可能出现支付成功，但是订单状态未改变）
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizNo(orderId);

        if (!ObjectUtil.isEmpty(payOrderDTO) && payOrderDTO.getStatus() == 3) {
            // 2.1 支付流水不为空且已支付，则标记订单状态为已支付
            orderService.markOrderPaySuccess(orderId);
        } else {
            // 2.2 如果订单流水不存在或者未支付，则取消订单
            orderService.cancelOrder(orderId);
        }


    }
}
