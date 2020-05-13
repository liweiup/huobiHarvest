package com.contract.harvest.controller;

import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.HuobiService;
import com.contract.harvest.service.TaskService;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
@ControllerAdvice
public class bi_info {

    @Autowired
    private HuobiService huobiService;
    @Autowired
    private HuobiEntity huobiEntity;

    @Qualifier("harvestExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private TaskService taskService;

    @RequestMapping("get_bi_info")
    @ResponseBody
    public String get_bi_info(@RequestParam String symbol,
                              @RequestParam String contract_type,
                              @PathVariable(required = false) String contract_code) throws Exception {
        String bi_info = huobiService.invoke_bi_info(symbol,contract_type,contract_code);
        return bi_info;
    }

//    @RequestMapping("show_harvest_log")
//    @ResponseBody
//    public String get_harvest_log(@RequestParam String date) {
//
//        return bi_info;
//    }

    @RequestMapping("getPriceLimit")
    @ResponseBody
    public String getPriceLimit(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String priceLimit = huobiEntity.getPriceLimit(Params.get("symbol"),Params.get("contract_type"),"");
        return priceLimit;
    }

    @RequestMapping("getContractIndex")
    @ResponseBody
    public String getContractIndex(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String contractIndex = huobiEntity.getContractIndex(Params.get("symbol"));
        return contractIndex;
    }


    @RequestMapping("getOpenInterest")
    @ResponseBody
    public String getOpenInterest(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String getOpenInterest = huobiEntity.getOpenInterest(Params.get("symbol"),Params.get("contract_type"),"");
        return getOpenInterest;
    }
    @RequestMapping("getMarketDetailMerged")
    @ResponseBody
    public String getMarketDetailMerged(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String detailMerged = huobiEntity.getMarketDetailMerged(Params.get("symbol"));
        return detailMerged;
    }
    @RequestMapping("getMarketHistoryTrade")
    @ResponseBody
    public String getMarketHistoryTrade(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String marketHistoryTrade = huobiEntity.getMarketHistoryTrade(Params.get("symbol"),Params.get("size"));
        return marketHistoryTrade;
    }
    @RequestMapping("getContractAccountInfo")
    @ResponseBody
    public String getContractAccountInfo(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String accountInfo = huobiEntity.getContractAccountInfo(Params.get("symbol"));
        return accountInfo;
    }

    @RequestMapping("getContractHisbasisAll")
    @ResponseBody
    public String getContractHisbasisAll(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String accountInfo = huobiEntity.getContractHisbasisAll(Params.get("symbol"), Params.get("period"), Params.get("basis_price_type"), Params.get("size"));
        return accountInfo;
    }

    @RequestMapping("getContractElitePositionRatio")
    @ResponseBody
    public String getContractElitePositionRatio(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String accountInfo = huobiEntity.getContractElitePositionRatio(Params.get("symbol"),Params.get("period"));
        return accountInfo;
    }

    @RequestMapping("getContractEliteAccountRatio")
    @ResponseBody
    public String getContractEliteAccountRatio(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String accountInfo = huobiEntity.getContractEliteAccountRatio(Params.get("symbol"),Params.get("period"));
        return accountInfo;
    }
    @RequestMapping("getContractPositionInfo")
    @ResponseBody
    public String getContractPositionInfo(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        String accountInfo = huobiEntity.getContractAccountPositionInfo(Params.get("symbol"));
        return accountInfo;
    }


    @RequestMapping("invoke_bi")
    @ResponseBody
    public void invoke_bi(@RequestParam Map<String,String> Params) throws Exception {
        int num = 0;
        while (true) {
//            Thread.sleep(2000);
            taskService.exec_invoke_bi(Params,num);
//            System.out.println("线程池中线程数目："+taskExecutor.getThreadPoolExecutor().getPoolSize()+"，队列中等待执行的任务数目："+
//                taskExecutor.getThreadPoolExecutor().getQueue().size()+"，已执行玩别的任务数目："+taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
            num++;
        }
    }

    @RequestMapping("index")
    public String index() {
        Map<String,String> Params = new HashMap<>();
        Params.put("flag","ddd");
        return "index";
    }

    //查出用户数据，在页面展示
    @RequestMapping("/success")
    public String success(Map<String,Object> map){
        map.put("hello","<h1>你好</h1>");
        map.put("users", Arrays.asList("zhangsan","lisi","wangwu"));
        return "success";
    }
    @RequestMapping("/getContractHisorders")
    @ResponseBody
    public String getContractHisorders(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        System.out.println(Params);
        String accountInfo = huobiEntity.getContractHisorders(Params.get("symbol"),Params.get("trade_type"),Params.get("type"),Params.get("status"),Params.get("create_date"),Params.get("page_index"),Params.get("page_size"));
        return accountInfo;
    }


}
