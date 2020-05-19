package com.contract.harvest.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.tools.Arith;
import com.huobi.common.request.Order;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VerifyParams {

    @Autowired
    private HuobiEntity huobiEntity;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MailService mailService;

    @Value("${space.buy_small_vol}")
    private int buy_small_vol;

    @Value("${space.default_week_type}")
    private String default_week_type;
    //交易周期
    private final String next_week = "qn";
    private final String now_week = "qc";

    //如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
    private final String quarter_flag = "_CQ";
    private final String nw_flag = "_NW";
    private final String cw_flag = "_CW";

    private static final Logger logger = LoggerFactory.getLogger(VerifyParams.class);

    /**
     * 获取基差的百分比与具体基差
     */
    public Map<String, String> getBasisFlag(double priceOne, double priceTwo) {
        Map<String, String> priceMap = new HashMap<>();
        if (Arith.compareNum(priceOne,priceTwo)) {
            priceMap.put("open", "-1");
            priceMap.put("percent", String.valueOf(Arith.div(Arith.sub(priceOne,priceTwo),priceOne)));
        } else {
            priceMap.put("open", "1");
            priceMap.put("percent", String.valueOf(Arith.div(Arith.sub(priceTwo,priceOne),priceTwo)));
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
            cq_contract_info = getContractInfo(symbol,"quarter","");
            nw_contract_info = getContractInfo(symbol,"next_week","");
            position_num = 0;//交易次数
            contract_code = symbol+quarter_flag+"-"+symbol+nw_flag;
            priceMap.put("contract_code",contract_code);
            priceMap.put("price_flag",default_week_type);
        }else{
            //交易周期
            String[] deal_flag_arr = deal_flag.split("-");
            //交易次数
            position_num = cacheService.get_deal_order_queue_num(deal_flag);
            //合约代码
            if (deal_flag_arr.length != 2)
            {
                logger.error("交易代码出错:["+ Arrays.toString(deal_flag_arr) +"]");
                mailService.sendMail("交易代码出错",Arrays.toString(deal_flag_arr),"");
                return null;
            }
            //合约信息
            cq_contract_info = getContractInfo(symbol,"",deal_flag_arr[0]);
            nw_contract_info = getContractInfo(symbol,"",deal_flag_arr[1]);
            //如果是季度合约
            if (cq_contract_info.getString("contract_type").equals("quarter")) {
                //如果是次周合约
                if (nw_contract_info.getString("contract_type").equals("next_week")) {
                    priceMap.put("contract_code",symbol+quarter_flag+"-"+symbol+nw_flag);
                    priceMap.put("price_flag",next_week);
                    //如果是本周合约
                }else if(nw_contract_info.getString("contract_type").equals("this_week")) {
                    priceMap.put("contract_code",symbol+quarter_flag+"-"+symbol+cw_flag);
                    priceMap.put("price_flag", now_week);
                }else {
                    logger.error("nw_contract_info_error:["+nw_contract_info.toJSONString()+"]");
                    return null;
                }
            } else {
                logger.error("交易周期错误:["+cq_contract_info.toJSONString()+"]");
                mailService.sendMail("交易周期错误",cq_contract_info.toJSONString(),"");
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
    public List<Order> getListOrder(String symbol,double basis_percent,String volume,String offset,String lever_rate,
                                    String order_price_type,String[] contract_code_arr,String quarter_direction,
                                    String week_direction,String cq_contract_type,String nc_contract_type)
            throws IOException, HttpException, InterruptedException {
        List<Order> orders = new ArrayList<>();
        Map<String, Double> dealPriceMap = getDealPrice(contract_code_arr[0],contract_code_arr[1],quarter_direction,week_direction);
        //二次验证基差百分比
        Map<String, String> basisPriceMap = getBasisFlag(dealPriceMap.get("quarterPrice"),dealPriceMap.get("ncPrice"));
        double sure_basis_percent = Double.parseDouble(basisPriceMap.get("percent"));
        if (offset.equals("open")) {
            if (Arith.compareNum(basis_percent,sure_basis_percent))
            {
                logger.error("开仓基差确认有误:[基差:"+Arith.getStrBigDecimal(basis_percent)+"-->"+Arith.getStrBigDecimal(sure_basis_percent)+dealPriceMap.get("quarterPrice")+"周:"+dealPriceMap.get("ncPrice")+"]");
                return orders;
            }
        }else if (offset.equals("close")) {
            if (Arith.compareNum(sure_basis_percent,basis_percent))
            {
                logger.error("平仓基差确认有误:[基差:"+Arith.getStrBigDecimal(basis_percent)+"-->"+Arith.getStrBigDecimal(sure_basis_percent)+"季度:"+dealPriceMap.get("quarterPrice")+"周:"+dealPriceMap.get("ncPrice")+"]");
                return orders;
            }
        }
        //获取开平仓挂单队列是否有订单
        Long order_queue_len = cacheService.get_redis_key_order_queue_len(offset);
        if (order_queue_len > 0) {
            logger.info("已有订单等待处理,币:"+symbol+"offset:"+offset);
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
        String ncPrice = String.valueOf(dealPriceMap.get("ncPrice"));
        Order order_quarter = new Order(symbol, "", cq_contract_type, "", quarterPrice, volume, quarter_direction, offset, lever_rate, order_price_type);
        Order order_week = new Order(symbol, "", nc_contract_type, "", ncPrice, volume, week_direction, offset, lever_rate, order_price_type);
        orders.add(order_quarter);
        orders.add(order_week);
        return orders;
    }
    /**
     * 获取可平仓位与方向
     */
    public Map<String, String> getContractAccountPositionInfo(String symbol) throws IOException, HttpException {
        String positionInfo = huobiEntity.getContractAccountPositionInfo(symbol);
        JSONObject positions = (JSONObject) JSONObject.parseObject(positionInfo).getJSONArray("data").get(0);
        JSONArray positionsArr = positions.getJSONArray("positions");
        Map<String,String> flagDirection = new HashMap<>();
        if (positionsArr.size() == 0) {
            return flagDirection;
        }
        double profit = 0;
        int sumVolume = 0;
        for (Object position:positionsArr) {
            JSONObject positionObj = JSONObject.parseObject(String.valueOf(position));
            String contract_code = positionObj.getString("contract_code");
            String contract_type = positionObj.getString("contract_type");
            String direction = positionObj.getString("direction");
            String volume = String.valueOf(positionObj.getIntValue("volume"));
            flagDirection.put(contract_code,direction.equals("sell") ? "buy" : "sell");
            flagDirection.put(contract_type,flagDirection.get(contract_code));
            flagDirection.put("volume",volume);
            profit = Arith.add(profit,positionObj.getDoubleValue("profit"));
            sumVolume += positionObj.getIntValue("volume");
        }
        flagDirection.put("sumVolume",String.valueOf(sumVolume));
        flagDirection.put("profit",String.valueOf(profit));
        return flagDirection;
    }
    /**
     * 获取买一价格和卖一价格
     * @param symbol_cq         币种
     * @param quarter_direction 季度购买方向
     * @param week_direction    周购买方向
     */
    public Map<String, Double> getDealPrice(String symbol_cq,String symbol_nc,String quarter_direction, String week_direction) throws IOException, HttpException, InterruptedException {
        //获取买一价和卖一价进行交易
        String quarterInfo = huobiEntity.getMarketDetailMerged(symbol_cq);
        String nwInfo = huobiEntity.getMarketDetailMerged(symbol_nc);
        JSONObject quarterInfoJsonObj = JSONObject.parseObject(quarterInfo);
        JSONObject nwInfoJsonObj = JSONObject.parseObject(nwInfo);
        String quarterPriceFlag,selWeekPriceFlag;
        quarterPriceFlag = quarter_direction.equals("buy") ? "ask" : "bid";
        selWeekPriceFlag = week_direction.equals("buy") ? "ask" : "bid";
        JSONArray quarterPrice = quarterInfoJsonObj.getJSONObject("tick").getJSONArray(quarterPriceFlag);
        JSONArray ncPrice = nwInfoJsonObj.getJSONObject("tick").getJSONArray(selWeekPriceFlag);
        if (Arith.compareNum(quarterPrice.getIntValue(1),buy_small_vol) || Arith.compareNum(ncPrice.getIntValue(1),buy_small_vol)) {
            Thread.sleep(200);
            return getDealPrice(symbol_cq,symbol_nc,quarter_direction,week_direction);
        }
        Map<String, Double> dealPriceMap = new HashMap<>();
        dealPriceMap.put("quarterPrice", quarterPrice.getDoubleValue(0));
        dealPriceMap.put("ncPrice", ncPrice.getDoubleValue(0));
        return dealPriceMap;
    }

    /**
     * 获取季度，次周，本周合约价格
     */
    public Map<String, Double> getContractAllPrice(String symbol, String price_flag,Long position_num) throws IOException, HttpException, InterruptedException, NullPointerException,IndexOutOfBoundsException {
        Map<String, Double> contractAllPrice = new HashMap<>();
        String quarterInfo = huobiEntity.getMarketDetailMerged(symbol+quarter_flag);
        String selWeekFlag = price_flag.equals(next_week) ? symbol+nw_flag : symbol+cw_flag;
        String selWeekInfo = huobiEntity.getMarketDetailMerged(selWeekFlag);
        //获取整合基差数据
        String contractHisbasisAll = huobiEntity.getContractHisbasisAll(symbol,"1min","close","1");
        JSONArray contractHisbasisAllArr = JSONObject.parseObject(contractHisbasisAll).getJSONArray("data");
        if (contractHisbasisAllArr.isEmpty()) {
            Thread.sleep(1000);
            return getContractAllPrice(symbol,price_flag,position_num);
        }
        JSONObject contractHisbasisAllObj = (JSONObject) contractHisbasisAllArr.get(0);
        String selWeekPriceKey = price_flag.equals(next_week) ? "contract_price_nw" : "contract_price_cw";
        double quarterPrice = contractHisbasisAllObj.getDoubleValue("contract_price_cq");
        double selWeekPrice = contractHisbasisAllObj.getDoubleValue(selWeekPriceKey);

        String quarterPriceFlag = Arith.compareNum(quarterPrice,selWeekPrice) ? "bid" : "ask";
        String selWeekPriceFlag = quarterPriceFlag.equals("ask") ? "bid" : "ask";
        JSONObject quarterTick = JSONObject.parseObject(quarterInfo).getJSONObject("tick");
        JSONObject selWeekTick = JSONObject.parseObject(selWeekInfo).getJSONObject("tick");

        JSONArray quarterPriceArr = quarterTick.getJSONArray(quarterPriceFlag);
        JSONArray selWeekPriceArr = selWeekTick.getJSONArray(selWeekPriceFlag);

        if (Arith.compareNum(quarterPriceArr.getIntValue(1), buy_small_vol) || Arith.compareNum(selWeekPriceArr.getIntValue(1), buy_small_vol)) {
            Thread.sleep(200);
            return getContractAllPrice(symbol,price_flag,position_num);
        }
        /*
          开平仓价格
          季度想开空单，周想开多单。 比对季度买入价格与次周卖出价格的基差。
          季度想开多单，周想开空单。 比对季度卖出价格与次周买入价格的基差。
         */
        contractAllPrice.put("quarterPrice",quarterPriceArr.getDoubleValue(0));
        contractAllPrice.put("qcPrice",selWeekPriceArr.getDoubleValue(0));
        //平仓买卖价和开仓买卖价相反
        String quarterCloseFlag = quarterPriceFlag.equals("bid") ? "ask" : "bid";
        String weekCloseFlag = quarterCloseFlag.equals("bid") ? "ask" : "bid";
        JSONArray quarterClosePriceArr = quarterTick.getJSONArray(quarterCloseFlag);
        JSONArray weekClosePriceArr = selWeekTick.getJSONArray(weekCloseFlag);
        contractAllPrice.put("quarterClosePrice",quarterClosePriceArr.getDoubleValue(0));
        contractAllPrice.put("qcClosePrice",weekClosePriceArr.getDoubleValue(0));
        return contractAllPrice;
    }

    /**
     * 获取合约信息
     */
    public JSONObject getContractInfo(String symbol,String contract_type,String deal_flag) throws IOException, HttpException {
        String cq_contract_info = huobiEntity.getContractInfo("","","");
        JSONArray contract_info_arr = JSONObject.parseObject(cq_contract_info).getJSONArray("data");
        for (Object contract_info:contract_info_arr) {
            JSONObject contract_info_obj = (JSONObject) contract_info;
            if (symbol.equals(contract_info_obj.getString("symbol")) && contract_type.equals(contract_info_obj.getString("contract_type"))) {
                return contract_info_obj;
            }
            if (contract_info_obj.getString("contract_code").equals(deal_flag)) {
                return contract_info_obj;
            }
        }
        return null;
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
        List<String> order_arr = new ArrayList<>();
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
}