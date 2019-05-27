package com.my.seckill;

import com.alibaba.fastjson.JSONObject;
import com.my.seckill.service.GoodsService;
import com.my.seckill.util.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SeckillApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(SeckillApplicationTests.class);

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    GoodsService goodsService;

    Integer secKillId = 355;
    Long goodsId = 45643L;
    Integer goodsCount = 10;//此次秒杀共10件商品

    @Test
    public void loadGoodsInfo() {
        redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "stock", goodsCount);
        redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "finish", false);
        redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "startTime", System.currentTimeMillis() + 1000 * 60);
    }

    @Test
    public void goodsStockTest() {
        redisTemplate.opsForValue().set(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":stock", 10);
    }

    @Test
    public void getGoodsInfo() {
        Integer stock = (Integer) redisTemplate.opsForHash().get(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "stock");
        System.out.println("****************" + stock);
    }

    @Test
    public void createUserIdSet() {
        redisTemplate.opsForSet().add(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users", -1L);
//        Long userId = (Long) redisTemplate.opsForSet().pop(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users");
//        System.out.println("*********************" + userId);
    }

    @Test
    public void sizeTest() {
        Long size = redisTemplate.opsForSet().size(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users");
        System.out.println("*****************" + size);
    }

    @Test
    public void secKillTest() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future> futureList = new CopyOnWriteArrayList<>();
        for (long i = 10000L; i < 20000L; i++) {
            long finalI = i;
            Future future = executorService.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (finalI < 10) {
                    JSONObject ret = goodsService.secKill(secKillId, goodsId, 1L);
                } else {
                    JSONObject ret = goodsService.secKill(secKillId, goodsId, finalI);
                }

//                log.info(ret.toJSONString());
            });
            futureList.add(future);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time = System.currentTimeMillis();
        countDownLatch.countDown();

        for (Future f : futureList) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        long cost = System.currentTimeMillis() - time;
        log.info("**************cost:" + cost);

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void handleOrderTest() {
        goodsService.handleSecKillOrder();
    }
}
