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
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.*;
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

    private VoucherOrderService proxy;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT ;
    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua")) ;
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //异步处理线程池   创立了一个单线程线程池，专门用于处理秒杀订单
//订单按顺序处理，同一个JVM内不会有多个线程同时写订单 但数据库落库串行执行，吞吐量受单个消费者限制
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();


    //在类初始化之后执行
    @PostConstruct
    private void init() {
    //向线程池提交一个长期运行的消费者任务
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    //消费者不断读取阻塞队列
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息，封装为VoucherOrder实体类
                    //队列为空时，take()会让后台线程等待，不会让while(true)空转消耗CPU
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            //1.获取用户
            Long userId = voucherOrder.getUserId();
            // 2.创建锁对象
            RLock redisLock = redissonClient
                    .getLock(RedisConstants.ORDER_LOCK_KEY + userId);
            // 3.尝试获取锁
            boolean isLock = redisLock.tryLock();
            // 4.判断是否获得锁成功
            if (!isLock) {
                // 获取锁失败，直接返回失败或者重试
                log.error("不允许重复下单！");
                return;
            }
            try {
                //注意：避免SpringAOP的自调用问题，使用代理对象调用方法使事务生效
                proxy.createVoucherOrder(voucherOrder);
            } finally {
                // 释放锁
                redisLock.unlock();
            }
        }

        //阻塞队列  用final修饰防止引用被重新赋值
        private final BlockingQueue<VoucherOrder> orderTasks =new ArrayBlockingQueue<>(1024 * 1024);

        @Override
        public Result<Long> seckillVoucher(Long voucherId) {
            //用户id
            Long userId = UserHolder.getUser().getId();
            //订单id
            long orderId = redisIdWorker.nextId("order");
            // 执行lua脚本
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Arrays.asList(RedisConstants.SECKILL_STOCK_KEY+voucherId,
                            RedisConstants.SECKILL_USER_KEY+voucherId),
                    userId.toString()
            );
            int r = result.intValue();
            // 判断结果是否为0
            if (r != 0) {
                // 不为0 ，代表没有购买资格
                return Result.error(r == 1 ? "库存不足" : "不能重复下单");
            }
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            // 用户id
            voucherOrder.setUserId(userId);
            // 代金券id
            voucherOrder.setVoucherId(voucherId);
            //获取代理对象  用于调用带@Transactional的方法
            proxy = (VoucherOrderService)AopContext.currentProxy();
            // 放入阻塞队列
            orderTasks.add(voucherOrder);

            //返回订单id
            return Result.success(orderId);
        }

        @Override
        @Transactional
        public  void createVoucherOrder(VoucherOrder voucherOrder) {
            Long userId = voucherOrder.getUserId();
            // 5.1.查询订单
            long count = query().eq("user_id", userId)
                    .eq("voucher_id", voucherOrder.getVoucherId()).count();
            // 5.2.判断是否存在
            if (count > 0) {
                // 用户已经购买过了
                log.error("用户已经购买过了");
                return ;
            }

            // 6.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)
                    .update();
            if (!success) {
                // 扣减失败
                log.error("库存不足");
                return ;
            }
            save(voucherOrder);

        }




    }


