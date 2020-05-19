package com.contract.harvest.controller;

import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.ElesService;
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

@RestController
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

    @Autowired
    private ElesService elesService;


    @RequestMapping("get_bi_info")
    @ResponseBody
    public String get_bi_info(@RequestParam String symbol,
                              @RequestParam String contract_type,
                              @PathVariable(required = false) String contract_code) throws Exception {
        String bi_info = huobiService.invoke_bi_info(symbol,contract_type,contract_code);
        return bi_info;
    }

    @RequestMapping("show_harvest_log")
    public StringBuffer get_harvest_log(@RequestParam String date) throws IOException {
        return elesService.get_harvest_log(date);
    }

    @RequestMapping("getPriceLimit")
    public String getPriceLimit(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getPriceLimit(Params.get("symbol"),Params.get("contract_type"),"");
    }

    @RequestMapping("getContractIndex")
    public String getContractIndex(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractIndex(Params.get("symbol"));
    }


    @RequestMapping("getOpenInterest")
    public String getOpenInterest(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getOpenInterest(Params.get("symbol"),Params.get("contract_type"),"");
    }
    @RequestMapping("getMarketDetailMerged")
    public String getMarketDetailMerged(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getMarketDetailMerged(Params.get("symbol"));
    }
    @RequestMapping("getMarketHistoryTrade")
    public String getMarketHistoryTrade(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getMarketHistoryTrade(Params.get("symbol"),Params.get("size"));
    }
    @RequestMapping("getContractAccountInfo")
    public String getContractAccountInfo(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractAccountInfo(Params.get("symbol"));
    }

    @RequestMapping("getContractHisbasisAll")
    public String getContractHisbasisAll(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractHisbasisAll(Params.get("symbol"), Params.get("period"), Params.get("basis_price_type"), Params.get("size"));
    }

    @RequestMapping("getContractElitePositionRatio")
    public String getContractElitePositionRatio(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractElitePositionRatio(Params.get("symbol"),Params.get("period"));
    }

    @RequestMapping("getContractEliteAccountRatio")
    public String getContractEliteAccountRatio(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractEliteAccountRatio(Params.get("symbol"),Params.get("period"));
    }
    @RequestMapping("getContractPositionInfo")
    public String getContractPositionInfo(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractAccountPositionInfo(Params.get("symbol"));
    }

    @RequestMapping("/getContractHisorders")
    public String getContractHisorders(@RequestParam Map<String,String> Params) throws IOException, HttpException {
        return huobiEntity.getContractHisorders(Params.get("symbol"),Params.get("trade_type"),Params.get("type"),Params.get("status"),Params.get("create_date"),Params.get("page_index"),Params.get("page_size"));
    }


}
