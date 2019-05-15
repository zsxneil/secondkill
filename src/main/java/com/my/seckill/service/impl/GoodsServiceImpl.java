package com.my.seckill.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.my.seckill.service.GoodsService;
import com.my.seckill.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GoodsServiceImpl implements GoodsService {
    private static final Logger log = LoggerFactory.getLogger(GoodsServiceImpl.class);

    @Autowired
    RedisTemplate redisTemplate;

    /**
     *
     * @param secKillId 活动id
     * @param goodsId   商品id
     * @param userId    用户id
     * @return
     */
    @Override
    public JSONObject secKill(Integer secKillId, Long goodsId, Long userId) {
        JSONObject ret = new JSONObject();

        //快速判断
        Long queueNum = Long.parseLong(String.valueOf(redisTemplate.opsForValue().get(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":queuenum")));
        if (queueNum >= 10) { //这里的库存数量应该先加载到内存

            //超过数量后，先查看set中和hash中有没有该userId
            if (redisTemplate.opsForSet().isMember(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users", userId)) {
                ret.put("msg", "排队中");
                ret.put("status", 2);
                return ret;
            }
            if (redisTemplate.opsForHash().get(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":" + userId, "order") != null) {
                boolean orderSuccess = (boolean) redisTemplate.opsForHash().get(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":" + userId, "order");
                if (orderSuccess) {
                    ret.put("msg", "秒杀成功");
                    ret.put("status", 0);
                } else {
                    ret.put("msg", "排队中");
                    ret.put("status", 2);
                }
                return ret;
            }

            ret.put("msg", "活动结束");
            ret.put("status", 1);
            return ret;
        }

        //先判断是不是已经添加到set中，没有添加到set中的才继续执行
        if (!redisTemplate.opsForSet().isMember(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users", userId)) {
            queueNum = redisTemplate.opsForValue().increment(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":queuenum", 1);
            if (queueNum > 10) {
                ret.put("msg", "活动结束");
                ret.put("status", 1);
                return ret;
            }
            redisTemplate.opsForSet().add(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users", userId);
        }
        ret.put("msg", "处理中");
        ret.put("status", 2);
        return ret;
    }


    /**
     * 从set中取用户id，添加到 hash中,订单生成状态为false，减库存，生成订单，将hash中订单生成状态该为true
     *
     * 这个任务应该通过mq消息启动，主要是为了获取goodsId，seckillId等信息
     * 发送消息的时机是什么？
     * 任务启动的条件是什么？
     */
    @Override
    public void handleSecKillOrder() {
        Integer secKillId = 355;
        Long goodsId = 45643L;
        String setKey = Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":users";
        boolean start = false;
        while (true) {
            Long goodsStock =  Long.parseLong(String.valueOf(redisTemplate.opsForHash().get(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "stock")));
            Long size = redisTemplate.opsForSet().size(setKey);
            log.info("******************size:" + size);
            if (start || (size >= goodsStock)) { //这里最好判断一下，如果因为异常用户数超过了库存数，应该将多余的去掉
                start = true;
                //将set中的用户id，创建成hash，字段包含订单生成状态true/false
                Object userIdObj = redisTemplate.opsForSet().pop(setKey);
                if (userIdObj == null) {
                    //将活动的结束标识设置为true
                    redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId, "finish", true);
                    break;
                }

                Long userId = Long.parseLong(String.valueOf(userIdObj));
                log.info("userId:" + userId);

                redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":user:" + userId, "order", false);
                //操作数据库：减库存，生成订单。库存指秒杀系统的库存，订单需要调用订单服务，这里又涉及到了分布式事务
                try {
                    Thread.sleep(1000); //模拟耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                redisTemplate.opsForHash().put(Constants.REDIS_PREFIX + secKillId + ":" + goodsId + ":user:" + userId, "order", true);
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        log.info("所有set处理结束");
    }
}
