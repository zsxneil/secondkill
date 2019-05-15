package com.my.seckill.controller;

import com.alibaba.fastjson.JSONObject;
import com.my.seckill.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoodsController {

    @Autowired
    GoodsService goodsService;

    @GetMapping("/seckill/{seckillId}/goods/{goodsId}")
    public JSONObject seckill(@PathVariable("seckillId") Integer secKillId,
                              @PathVariable("goodsId") Long goodsId,
                              @RequestParam("userId") Long userId) //用户id应该是从上下文中获取的，这里是模拟，使用客户端直接传入
    {
        return goodsService.secKill(secKillId, goodsId, userId);
    }

}
