package com.contract.harvest.service;

import com.contract.harvest.tools.Arith;
import com.contract.harvest.tools.CodeConstant;
import com.mysql.cj.protocol.x.Notice;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service
public class ScheduledService {

    @Resource
    private RedisService redisService;

    @Resource
    private CacheService cacheService;

    @Resource
    private MailService mailService;

    @Resource
    private VerifyParams verifyParams;

    @Resource
    private TaskService taskService;

    @Qualifier("harvestExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${space.basis_percent_after}")
    private double basis_percent_after;
    //获取所有币名
    public Set<Object> getSymbol() {
        return redisService.getSetMembers(CacheService.redis_key_symbol_flag);
    }
    //获取所有成交基差
    public List<Object> getSymbolAllPercent(String symbol) {
        String key = CacheService.redis_key_percent_flag+":"+symbol;
        return redisService.lrangeList(key,0,-1);
    }
    /**
     * 检测订单成交数是否一致
     */
    @Scheduled(cron = "0 */10 * * * ?")  //每一小时执行一次
    public void checkDealOrder() {
        for (Object symbolObj : getSymbol()) {
            String symbol = String.valueOf(symbolObj);
            String deal_flag = cacheService.get_redis_key_deal_flag(symbol);
            //获取成交订单长度
            Long queueLen = cacheService.get_deal_order_queue_num(deal_flag);
            //获取成交基差长度
            Long percentFlagLen = redisService.getListLen(CacheService.redis_key_percent_flag+":"+symbol);
            if (!queueLen.equals(percentFlagLen)) {
                String info_str = "订单成交数："+queueLen + "成交基差数" + percentFlagLen;
                mailService.sendMail(CodeConstant.getMsg(CodeConstant.DEAL_ORDER_QUEUE_ERROR),info_str,"");
            }
        }
    }
    /**
     * 成交订单数大于 2
     * 基差和小于 成交次数 * 第二次及之后开仓基差百分比
     * 做订单合并
     */
    @Scheduled(cron = "0 */30 * * * ?")  //每30分钟执行一次
    public void checkDealOrderAndMerge() {
        for (Object symbolObj : getSymbol()) {
            String symbol = String.valueOf(symbolObj);
            String deal_flag = cacheService.get_redis_key_deal_flag(symbol);
            //获取成交订单长度
            Long queueLen = cacheService.get_deal_order_queue_num(deal_flag);
            if (queueLen < 2) {
                continue;
            }
            //获取所有成交基差
            double allPercent = 0;
            List<Object> symbolAllPercent = getSymbolAllPercent(symbol);
            for (Object percentStr:symbolAllPercent) {
                allPercent = Arith.add(allPercent,Double.parseDouble(String.valueOf(percentStr)));
            }
            double flag_percent = Arith.mul(basis_percent_after,queueLen);
            if (Arith.compareNum(flag_percent,allPercent)) {
                double realPercent = Arith.div(allPercent,queueLen);
                //修剪订单，并重新设置基差
                redisService.listTrim(CacheService.redis_key_order_queue_deal+":"+deal_flag,0,0);
                redisService.lpush(CacheService.redis_key_percent_flag+":"+symbol,String.valueOf(realPercent));
                redisService.listTrim(CacheService.redis_key_percent_flag+":"+symbol,0,0);
                String info_str = "基差数组："+symbolAllPercent + "修改为" + realPercent;
                mailService.sendMail("合并基差",info_str,"");
            }
        }
    }

    /**
     * 多仓和空仓数量不一致
     */
    @Scheduled(cron = "0 */5 * * * ?")  //每5分钟执行一次
    public void checkPositionInfo() throws IOException, HttpException {
        for (Object symbolObj : getSymbol()) {
            String symbol = String.valueOf(symbolObj);
            Map<String,String> flagDirection = verifyParams.getContractAccountPositionInfo(symbol);
            if (flagDirection.isEmpty()) {
                continue;
            }
            int sumVolume = Integer.parseInt(flagDirection.get("sumVolume"));
            String info_str = "总计张数"+sumVolume;
            if ((sumVolume % 2) > 0) {
                mailService.sendMail("多仓空仓数量不一致",info_str,"");
            }
        }
    }

    @Scheduled(cron = "*/2 * * * * ?")  //每2秒执行一次
    public void invoke_bi() throws Exception {
        for (Object symbolObj : getSymbol()) {
            String symbol = String.valueOf(symbolObj);
            int num = 0;
            Thread.sleep(1000);
            Map<String,String> Params = new HashMap<>();
            Params.put("symbol",symbol);
            taskService.exec_invoke_bi(Params, num);
//            System.out.println("线程池中线程数目："+taskExecutor.getThreadPoolExecutor().getPoolSize()+"，队列中等待执行的任务数目："+
//                taskExecutor.getThreadPoolExecutor().getQueue().size()+"，已执行玩别的任务数目："+taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
        }
    }
//
//    @Scheduled(cron = "*/1 * * * * ?")
//    public void restTemplateGetTest(){
//        RestTemplate restTemplate = new RestTemplate();
//        String notice = restTemplate.getForObject("http://ym.api.com/index/download_kline_bian"
//                , String.class);
//        System.out.println(notice);
//    }
//    @Scheduled(cron = "*/1 * * * * ?")
//    public void restTemplateGetTest15(){
//        RestTemplate restTemplate = new RestTemplate();
//        String notice = restTemplate.getForObject("http://ym.api.com/Index/download_kline_bian15_min"
//                , String.class);
//        System.out.println(notice);
//    }

}
