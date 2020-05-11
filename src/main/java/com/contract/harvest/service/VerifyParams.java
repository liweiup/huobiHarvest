package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.entity.HuobiEntity;
import com.huobi.common.request.Order;
import org.apache.commons.collections.map.DefaultedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VerifyParams {

    @Autowired
    private HuobiEntity huobiEntity;

    @Autowired
    private CacheService cacheService;

    private static final Logger logger = LoggerFactory.getLogger(HuobiService.class);

    /**
     * 获取基差的百分比与具体基差
     */
    public Map<String, String> getBasisFlag(double priceOne, double PriceTwo) {
        Map<String, String> priceMap = new HashMap<>();
        if (priceOne > PriceTwo) {
            priceMap.put("open", "-1");
            priceMap.put("percent", String.format("%.4f", (priceOne - PriceTwo) / priceOne));

        } else if (PriceTwo > priceOne) {
            priceMap.put("open", "1");
            priceMap.put("percent", String.format("%.4f", (PriceTwo - priceOne) / PriceTwo));
        } else {
            priceMap.put("open", "0");
            priceMap.put("percent","0");
        }
        //开仓方向
        int open_direction = Integer.parseInt(priceMap.get("open"));
        priceMap.put("quarter_direction", open_direction == -1 ? "sell" : "buy");
        priceMap.put("week_direction", open_direction == -1 ? "buy" : "sell");
        return priceMap;
    }

    /**
     * 校对合约
     * 选择季度&次周 or 季度&本周
     */
    public Map<String, String> checkContractDeal(String symbol) throws IOException, HttpException {

        Map<String,String> priceMap = new HashMap<String,String>();
        //合约信息
        JSONObject cq_contract_info,nw_contract_info;
        //有无正在进行的交易 合约代码
        String deal_flag = cacheService.get_redis_key_deal_flag(symbol);
        //开仓次数
        long position_num;
        //合约标识，用来获取买卖价格
        String contract_code;
        if (deal_flag.equals("")) {
            cq_contract_info = this.getContractInfo(symbol,"quarter","");
            nw_contract_info = this.getContractInfo(symbol,"next_week","");
            position_num = 0;//交易次数
            priceMap.put("contract_code",symbol+"_CQ"+"-"+symbol+"_NW");
            priceMap.put("price_flag","qn");
        }else{
            //交易周期
            String[] deal_flag_arr = deal_flag.split("-");
            //交易次数
            position_num = cacheService.get_deal_order_queue_num(deal_flag);
            //合约代码
            if (deal_flag_arr.length != 2)
            {
                logger.error("deal_flag_arr.length():["+deal_flag_arr.length+"]");
                return null;
            }
            //合约信息
            cq_contract_info = this.getContractInfo(symbol,"",deal_flag_arr[0]);
            nw_contract_info = this.getContractInfo(symbol,"",deal_flag_arr[1]);
            //如果是季度合约
            if (cq_contract_info.getString("contract_type").equals("quarter")) {
                //如果是次周合约
                if (nw_contract_info.getString("contract_type").equals("next_week")) {
                    priceMap.put("contract_code",symbol+"_CQ"+"-"+symbol+"_NW");
                    priceMap.put("price_flag","qn");
                    //如果是本周合约
                }else if(nw_contract_info.getString("contract_type").equals("this_week")) {
                    priceMap.put("contract_code",symbol+"_CQ"+"-"+symbol+"_CW");
                    priceMap.put("price_flag","qc");
                }else {
                    logger.error("nw_contract_info_error:["+nw_contract_info.toJSONString()+"]");
                    return null;
                }
            }else {
                logger.error("cq_contract_info_error:["+cq_contract_info.toJSONString()+"]");
                return null;
            }
        }
        priceMap.put("cq_contract_type",String.valueOf(cq_contract_info.get("contract_code")));
        priceMap.put("nc_contract_type",String.valueOf(nw_contract_info.get("contract_code")));
        priceMap.put("position_num",String.valueOf(position_num));
        return priceMap;
    }

    /**
     * 获取下单实例
     * @param symbol 币种
     * @param basis_percent 基差
     * @param volume 张数
     * @param offset open or close
     * @param lever_rate 杠杆
     * @param order_price_type buy or sell
     * @param contract_code_arr 合约周期
     * @param quarter_direction 季度合约价格
     * @param week_direction 周合约价格
     * @param cq_contract_type 季度合约代码
     * @param nc_contract_type 周合约代码
     * @throws IOException
     * @throws HttpException
     */
    public List<Order> getListOrder(String symbol,float basis_percent,String volume,String offset,String lever_rate,
                                    String order_price_type,String[] contract_code_arr,String quarter_direction,
                                    String week_direction,String cq_contract_type,String nc_contract_type) throws IOException, HttpException, InterruptedException {
        List<Order> orders = new ArrayList();
        Map<String, Float> dealPriceMap = this.getDealPrice(contract_code_arr[0],contract_code_arr[1],quarter_direction,week_direction);
        //二次验证基差百分比
        Map<String, String> basisPriceMap = this.getBasisFlag(dealPriceMap.get("quarterPrice"),dealPriceMap.get("ncPrice"));
        float sure_basis_percent = Float.parseFloat(basisPriceMap.get("percent"));
//        if (!basisPriceMap.get("quarter_direction").equals(quarter_direction) || !basisPriceMap.get("week_direction").equals(week_direction)) {
//            logger.error("开平仓方向确认信息:["+sure_basis_percent+":"+basisPriceMap+"]");
//        }
        if (offset.equals("open")) {
            if (sure_basis_percent < basis_percent)
            {
                logger.error("开仓基差确认信息:[基差:"+sure_basis_percent+"季度:"+dealPriceMap.get("quarterPrice")+"周:"+dealPriceMap.get("ncPrice")+"]");
                return orders;
            }
        }else if (offset.equals("close")) {
            if (sure_basis_percent > basis_percent)
            {
                logger.error("平仓基差确认信息:[基差:"+sure_basis_percent+"季度:"+dealPriceMap.get("quarterPrice")+"周:"+dealPriceMap.get("ncPrice")+"]");
                return orders;
            }
        }
        //获取开仓挂单队列是否有订单
        Long order_queue_len = cacheService.get_redis_key_order_queue_len(offset);
        if (order_queue_len > 0) {
            logger.info("已有订单等待处理,币:"+symbol);
            Thread.sleep(1500);
            order_queue_len = cacheService.get_redis_key_order_queue_len(offset);
            if  (order_queue_len > 0) {
                String content = offset.equals("open") ? "handle_order" : "handle_close_order";
                //订阅通知
                cacheService.inform_sub("order_queue",content);
            }
            return orders;
        }
        String quarterPrice = String.valueOf(dealPriceMap.get("quarterPrice"));
        String nwPrice = String.valueOf(dealPriceMap.get("ncPrice"));
//        quarterPrice =  String.format("%.3f",Float.parseFloat(quarterPrice) * (1-0.05));
//        nwPrice =  String.format("%.3f",Float.parseFloat(nwPrice) * (1+0.061));
//        System.out.println(quarterPrice);
//        System.out.println(nwPrice);
//        System.exit(0);
        Order order_quarter = new Order(symbol, "", cq_contract_type, "", quarterPrice, volume, quarter_direction, offset, lever_rate, order_price_type);
        Order order_week = new Order(symbol, "", nc_contract_type, "", nwPrice, volume, week_direction, offset, lever_rate, order_price_type);
        orders.add(order_quarter);
        orders.add(order_week);
        return orders;
    }
    /**
     * 获取可平仓位与方向
     */
    public Map<String, String> getContractAccountPositionInfo(String symbol,String cq_contract_type,String nc_contract_type) throws IOException, HttpException {
        String positionInfo = huobiEntity.getContractAccountPositionInfo(symbol);
        JSONObject positions = (JSONObject) JSONObject.parseObject(positionInfo).getJSONArray("data").get(0);
        JSONArray positionsArr = positions.getJSONArray("positions");
        Map<String,String> flagDirection = new HashMap<String,String>();
        float profit = (float) 0;
        for (Object position:positionsArr) {
            JSONObject positionObj = JSONObject.parseObject(String.valueOf(position));
            String contract_code = positionObj.getString("contract_code");
            String direction = positionObj.getString("direction");
            String volume = String.valueOf(positionObj.getIntValue("volume"));
            flagDirection.put(contract_code,direction.equals("sell") ? "buy" : "sell");
            flagDirection.put("volume",volume);
            profit += positionObj.getFloatValue("profit");
        }
        flagDirection.put("profit",String.valueOf(profit));
//        System.out.println(flagDirection);
//        System.exit(0);
        return flagDirection;
    }
    /**
     * 获取买一价格和卖一价格
     * @param symbol_cq         币种
     * @param quarter_direction 季度购买方向
     * @param week_direction    周购买方向
     */
    public Map<String, Float> getDealPrice(String symbol_cq,String symbol_nc,String quarter_direction, String week_direction) throws IOException, HttpException {
        //获取买一价和卖一价进行交易
        String quarterInfo = huobiEntity.getMarketDetailMerged(symbol_cq);
        String nwInfo = huobiEntity.getMarketDetailMerged(symbol_nc);
        JSONObject quarterInfoJsonObj = JSONObject.parseObject(quarterInfo);
        JSONObject nwInfoJsonObj = JSONObject.parseObject(nwInfo);
        JSONArray quarterPrice = quarterInfoJsonObj.getJSONObject("tick").getJSONArray(quarter_direction.equals("buy") ? "bid" : "ask");
        JSONArray ncPrice = nwInfoJsonObj.getJSONObject("tick").getJSONArray(week_direction.equals("buy") ? "bid" : "ask");
        Map<String, Float> dealPriceMap = new HashMap<>();
        dealPriceMap.put("quarterPrice", quarterPrice.getFloatValue(0));
        dealPriceMap.put("ncPrice", ncPrice.getFloatValue(0));
        return dealPriceMap;
    }

    /**
     * 获取季度，次周，本周合约价格
     */
    public Map<String, Float> getContractAllPrice(String symbol,String price_flag) throws IOException, HttpException {

        String quarterInfo = huobiEntity.getMarketDetailMerged(symbol+"_CQ");
        String selWeekInfo = "";
        if (price_flag.equals("qn")) {
            selWeekInfo = huobiEntity.getMarketDetailMerged(symbol+"_NW");
        }else if (price_flag.equals("qc")) {
            selWeekInfo = huobiEntity.getMarketDetailMerged(symbol+"_CW");
        }
        //获取整合基差数据
        String contractHisbasisAll = huobiEntity.getContractHisbasisAll(symbol,"1min","close","1");
        JSONObject contractHisbasisAllObj = JSONObject.parseObject(contractHisbasisAll);
        float quarterPrice = contractHisbasisAllObj.getFloatValue("quarterPrice");
        float nwPrice = contractHisbasisAllObj.getFloatValue("nwPrice");
        float ncPrice = contractHisbasisAllObj.getFloatValue("ncPrice");

        float selWeekPrice = price_flag.equals("qn") ? nwPrice : ncPrice;

        String quarterPriceFlag = quarterPrice > selWeekPrice ? "ask" : "bid";
        String selWeekPriceFlag = selWeekPrice > quarterPrice ? "ask" : "bid";

        Map<String, Float> contractAllPrice = new HashMap<>();
        contractAllPrice.put("quarterPrice",JSONObject.parseObject(quarterInfo).getJSONObject("tick").getJSONArray(quarterPriceFlag).getFloatValue(0));
        contractAllPrice.put("qcPrice",JSONObject.parseObject(selWeekInfo).getJSONObject("tick").getJSONArray(selWeekPriceFlag).getFloatValue(0));
        return contractAllPrice;
    }

    /**
     * 获取合约信息
     */
    public JSONObject getContractInfo(String symbol,String contract_type,String deal_flag) throws IOException, HttpException {
        String cq_contract_info = huobiEntity.getContractInfo(symbol,contract_type,deal_flag);
        return (JSONObject) JSONObject.parseObject(cq_contract_info).getJSONArray("data").get(0);
    }
    /**
     * 获取订单id
     */
    public Map<String,String> getOrderIdStr(String order_info_str) throws NullPointerException{
        Map<String,String> OrderMap = new HashMap<>();
        JSONObject order_info_obj = JSONObject.parseObject(order_info_str);
        JSONObject order_data = (JSONObject) order_info_obj.get("data");
        String symbol = order_info_obj.getString("symbol");
        String offset = order_info_obj.getString("offset");
        String err_code = order_info_obj.getString("err_code");
        if (err_code != null) {
            return OrderMap;
        }
        List<String> order_arr = new ArrayList();
        Object[] success_order = order_data.getJSONArray("success").toArray();
        for (Object flag_order : success_order) {
            JSONObject order = (JSONObject)flag_order;
            order_arr.add(order.getString("order_id_str"));
        }
        String order_id_str =  StringUtils.join(order_arr,",");
        OrderMap.put("symbol",symbol);
        OrderMap.put("order_id_str",order_id_str);
        OrderMap.put("offset",offset);
        return OrderMap;
    }

    //获取当前时间
    public String getNowDatetime() {
        Date d = new Date();
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        return time.format(d);
    }

    /**
     * 科学计数法去除
     */
    public String getFloatNum(double d) {
        NumberFormat nf = NumberFormat.getInstance();
        // 是否以逗号隔开, 默认true以逗号隔开,如[123,456,789.128]
        nf.setGroupingUsed(false);
        // 结果未做任何处理
        return nf.format(d);
    }
}