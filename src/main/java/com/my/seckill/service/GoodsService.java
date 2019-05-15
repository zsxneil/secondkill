package com.my.seckill.service;

import com.alibaba.fastjson.JSONObject;

public interface GoodsService {

    JSONObject secKill(Integer secKillId, Long goodsId, Long userId);

    void handleSecKillOrder();
}
