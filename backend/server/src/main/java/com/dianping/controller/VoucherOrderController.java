package com.dianping.controller;


import com.dianping.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
@Tag(name = "优惠券订单接口")
public class VoucherOrderController {
    @PostMapping("seckill/{id}")
    @Operation(summary = "秒杀优惠券（功能未完成）")
    public Result<Void> seckillVoucher(@PathVariable("id") Long voucherId) {
        return Result.error("功能未完成");
    }
}
