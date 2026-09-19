package com.dianping.service.impl;

import com.dianping.constant.RedisConstants;
import com.dianping.dto.UserDTO;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.Voucher;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherMapper;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.result.Result;
import com.dianping.service.SeckillVoucherService;
import com.dianping.service.VoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.SimpleRedisLock;
import com.dianping.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {
    @Autowired
    private SeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public Result<Long> seckillVoucher(Long voucherId) {
        //查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId) ;
        if(seckillVoucher == null){
            return Result.error("优惠券不存在！") ;
        }
        //判断秒杀是否开始、是否结束
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.error("活动未开始！") ;
        }

        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.error("活动已结束！") ;
        }
        //判断库存是否充足
        if(seckillVoucher.getStock() < 1){
            return Result.error("库存不足！") ;
        }

        //用分布式锁综合处理一人一单
        Long userId = UserHolder.getUser().getId();
        //创建锁对象(新增代码)
        //SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId) ;
        //获取锁对象  这里不传入参数，一人一单要求获取失败直接返回
        boolean getLock = lock.tryLock();
        //加锁失败
        if (!getLock) {
            return Result.error("不允许重复下单");
        }
        //获取锁成功就创建订单
        try {
            //获取代理对象(事务)
            VoucherOrderService proxy = (VoucherOrderService) 	 		AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Transactional
    public  Result<Long> createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //直接用toString的话每次调用可能生成不同的String对象，导致同一用户的两个线程拿到不同的锁，无法互斥，intern()方法用于返回字符串常量池中的唯一对象

        // 查询订单
        Long count = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        // 判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.error("用户已经购买过一次！");
        }

        // 乐观锁解决超卖问题
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId)
                .gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.error("库存不足！");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户id
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 返回订单id
        return Result.success(orderId);
    }
}


