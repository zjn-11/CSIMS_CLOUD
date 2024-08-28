package com.zjn.pay.controller;

import cn.hutool.core.bean.BeanUtil;
import com.hmall.common.exception.BizIllegalException;
import com.zjn.api.dto.PayOrderDTO;
import com.zjn.pay.domain.dto.PayApplyDTO;
import com.zjn.pay.domain.dto.PayOrderFormDTO;
import com.zjn.pay.domain.po.PayOrder;
import com.zjn.pay.enums.PayType;
import com.zjn.pay.service.IPayOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "支付相关接口")
@RestController
@RequestMapping("pay-orders")
@RequiredArgsConstructor
public class PayController {

    private final IPayOrderService payOrderService;

    @ApiOperation("生成支付单")
    @PostMapping
    public String applyPayOrder(@RequestBody PayApplyDTO applyDTO){
        if(!PayType.BALANCE.equalsValue(applyDTO.getPayType())){
            // 目前只支持余额支付
            throw new BizIllegalException("抱歉，目前只支持余额支付");
        }
        return payOrderService.applyPayOrder(applyDTO);
    }

    @ApiOperation("尝试基于用户余额支付")
    @ApiImplicitParam(value = "支付单id", name = "id")
    @PostMapping("{id}")
    public void tryPayOrderByBalance(@PathVariable("id") Long id, @RequestBody PayOrderFormDTO payOrderFormDTO){
        payOrderFormDTO.setId(id);
        payOrderService.tryPayOrderByBalance(payOrderFormDTO);
    }

    @ApiOperation("基于订单号查询支付单")
    @ApiImplicitParam(value = "订单id", name = "id")
    @GetMapping("/biz/{id}")
    public PayOrderDTO queryPayOrderByBizNo(@PathVariable("id") Long id) {
        PayOrder payOrder = payOrderService.lambdaQuery().eq(PayOrder::getBizOrderNo, id).one();
        return BeanUtil.toBean(payOrder, PayOrderDTO.class);
    }
}
