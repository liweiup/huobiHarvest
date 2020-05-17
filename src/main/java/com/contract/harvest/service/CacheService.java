package com.contract.harvest.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CacheService {

    //开仓次数的key
    private String redis_key_position_num = "harvest:huobi:position";

    //订单队列key（开仓挂单）
    private String redis_key_order_queue = "harvest:huobi:order:queue:put";

    //订单队列key（平仓挂单）
    private String redis_key_order_close_queue = "harvest:huobi:order:queue:put:close";

    //订单队列key（错误挂单）
    private String redis_key_order_error_queue = "harvest:huobi:order:queue:error:put";

    //已经成交的订单队列key
    private String redis_key_order_queue_deal = "harvest:huobi:order:queue:deal";

    //已经成交的币的标识
    private String redis_key_deal_flag = "harvest:huobi:deal:flag";

    //当前下单的百分比
    private String redis_key_percent_flag = "harvest:huobi:percent:flag";

    //订单id放入set
    private String redis_key_order_exist_flag = "harvest:huobi:order:exist:flag";

    @Autowired
    private RedisService redisService;

    //设置下单百分比
    public void set_percent_flag(String symbol,String percent) {
        String percent_flag = redis_key_percent_flag+":"+symbol;
        redisService.rightPush(percent_flag, percent);
    }
    //移除下单百分比
    public void remove_percent_flag(String symbol,String offset) {
        String percent_flag = redis_key_percent_flag+":"+symbol;
        switch (offset){
            //移除一条
            case "close":
                redisService.rightPop(percent_flag);
                break;
            //全部移除
            case "close_all":
                redisService.deleteKey(percent_flag);
                break;
        }
    }
    //获取下单百分比
    public double get_percent_flag(String symbol) {
        String percent_flag = redis_key_percent_flag+":"+symbol;
        String percent_value = redisService.getLindex(percent_flag, (long) -1);
        if (percent_value.equals("null")) {
            return 0;
        }
        return Double.parseDouble(percent_value);
    }

    //是否已有成交
    public String get_redis_key_deal_flag(String symbol) {
        String key = redis_key_deal_flag+":"+symbol;
        if (!redisService.existsKey(key)) {
            return "";
        }
        return redisService.getValue(key);
    }
    //获取开仓次数
    public Integer get_position_num(String symbol) {
        String redis_key_symbol = redis_key_position_num+":"+symbol;
        String flag_position_num = redisService.getValue(redis_key_symbol);
        Integer position_num = flag_position_num == null ? 0 : Integer.parseInt(flag_position_num);
        return position_num;
    }
    //设置开仓次数
    public void set_position_num(String symbol,String position_num,Integer flag_num) {
        if (!StringUtils.isEmpty(position_num)) {
            position_num = String.valueOf(this.get_position_num(symbol) + flag_num);
        }
        String redis_key_symbol = redis_key_position_num+":"+symbol;
        redisService.setValue(redis_key_symbol,position_num);
    }
    /**
     * 订单入队列
     * @param symbol 币种
     * @param order_info 订单json字符串
     */
    public void order_insert_queue(String symbol,String offset,String order_info) {
        JSONObject contractObj = JSONObject.parseObject(order_info);
        contractObj.put("symbol",symbol);
        contractObj.put("offset",offset);
        switch (offset) {
            case "open":
                redisService.rightPush(redis_key_order_queue, contractObj.toJSONString());
                break;
            case "close":
            case "close_all":
                redisService.rightPush(redis_key_order_close_queue, contractObj.toJSONString());
                break;
        }
    }

    /**
     * 获取挂单队列的长度
     * @return
     */
    public Long get_redis_key_order_queue_len(String offset) {
        String redis_key = offset.equals("open") ? redis_key_order_queue : redis_key_order_close_queue;
        return redisService.getListLen(redis_key);
    }

    /**
     * 已成交的订单队列
     * @return
     */
    public void deal_order_queue(String contract_code,String orderDataStr,String symbol) {
        String new_key = redis_key_order_queue_deal+":"+contract_code;
        redisService.rightPush(new_key, orderDataStr);
    }
    /**
     * 将订单从已成交的订单队列中移除
     */
    public void remove_deal_order_queue(String contract_code,String offset) {
        String new_key = redis_key_order_queue_deal+":"+contract_code;
        switch (offset){
            //移除一条
            case "close":
                redisService.rightPop(new_key);
                break;
            //全部移除
            case "close_all":
                redisService.deleteKey(new_key);
                break;
        }
    }
    /**
     * 设置交易的币种周期
     */
    public void set_redis_key_deal_flag(String contract_code,String symbol) {
        redisService.setValue(redis_key_deal_flag+":"+symbol,contract_code);
    }
    /**
     * 删除交易的币种周期
     */
    public void del_redis_key_deal_flag(String symbol) {
        redisService.deleteKey(redis_key_deal_flag+":"+symbol);
    }
    /**
     * 已成交订单数量
     */
    public Long get_deal_order_queue_num(String contract_code) {
        String key = redis_key_order_queue_deal+":"+contract_code;
        return redisService.getListLen(key);
    }
    /**
     * 从队列中取一个订单
     */
    public String get_order_in_queue(String offset) {
        String order = "null";
        switch (offset) {
            case "open":
                order = redisService.getLindex(redis_key_order_queue, (long) 0);
                break;
            case "close":
                order = redisService.getLindex(redis_key_order_close_queue, (long) 0);
                break;
        }
        if (order.equals("null")) {
            return "";
        }
        return order;
    }
    /**
     * 从挂单队列中取移除一个挂单
     */
    public String lpop_order_in_queue(String offset) {
        String order = null;
        switch (offset) {
            case "open":
                order = String.valueOf(redisService.leftPop(redis_key_order_queue));
                break;
            case "close":
                order = String.valueOf(redisService.leftPop(redis_key_order_close_queue));
                break;
        }
        if (order.equals("null")) {
            return "";
        }
        return order;
    }
    /**
     * 从挂单队列中将订单取出并放到错误队列
     */
    public void lpop_order_to_error_queue(String offset) {
        String order = null;
        switch (offset) {
            case "open":
                order = String.valueOf(redisService.leftPop(redis_key_order_queue));
                break;
            case "close":
                order = String.valueOf(redisService.leftPop(redis_key_order_close_queue));
                break;
        }
        redisService.rightPush(redis_key_order_error_queue, order);
    }
    /**
     * 订阅通知
     */
    public void inform_sub(String channel_flag,String Content) {
        redisService.convertAndSend(channel_flag,Content);
    }

    /**
     * 订单id放入set
     */
    public Boolean getOrderIdStrIsExist(String order_info_str) {
        Long flag = redisService.addSet(redis_key_order_exist_flag,order_info_str);
        return flag > 0;
    }
}
