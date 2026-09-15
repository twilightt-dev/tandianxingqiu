package com.dianping.service;

import com.dianping.result.Result;
import com.dianping.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherService extends IService<Voucher> {

    Result<List<Voucher>> queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
