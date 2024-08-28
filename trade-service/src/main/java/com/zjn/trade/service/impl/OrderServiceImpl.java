package com.zjn.trade.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.zjn.api.client.CartClient;
import com.zjn.api.client.ItemClient;
import com.zjn.api.dto.ItemDTO;
import com.zjn.api.dto.OrderDetailDTO;
import com.zjn.trade.constants.MQConstants;
import com.zjn.trade.domain.dto.OrderFormDTO;
import com.zjn.trade.domain.po.Order;
import com.zjn.trade.domain.po.OrderDetail;
import com.zjn.trade.mapper.OrderMapper;
import com.zjn.trade.service.IOrderDetailService;
import com.zjn.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;
    private final OrderDetailServiceImpl orderDetailService;

    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        cartClient.removeByItemIds(itemIds);

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 5. 返送延迟消息，检查订单状态，确保超时或者为支付成功时能恢复库存
        rabbitTemplate.convertAndSend(
                MQConstants.DELAY_EXCHANGE_NAME,
                MQConstants.DELAY_ORDER_KEY,
                order.getId(),
                message -> {
            message.getMessageProperties().setDelay(30000);
            return message;
        });

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    /**
     * 1. 将订单状态修改为已关闭：5
     * 2. 恢复商品库存
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {
        // 1. 查询订单
        Order order = getById(orderId);
        if (ObjectUtil.isEmpty(order) || order.getStatus() == 5)
            return;
        // 2. 修改订单状态，并保存
        updateById(new Order().setId(orderId).setStatus(5).setUpdateTime(LocalDateTime.now()));
        // 3. 恢复库存
        // 3.1 查询出订单中所有的商品
        List<OrderDetail> orderDetails = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        if (ObjectUtil.isEmpty(orderDetails)) return;
        // 3.2 将商品数量变为负数，然后赋值给dto
        List<OrderDetailDTO> detailDTOS = orderDetails.stream().map(orderDetail -> {
            OrderDetailDTO dto = new OrderDetailDTO();
            dto.setItemId(orderDetail.getItemId());
            dto.setNum(orderDetail.getNum() * -1);
            return dto;
        }).collect(Collectors.toList());
        // 3.3 调用item-service的deductStock来改变库存
        itemClient.deductStock(detailDTOS);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
