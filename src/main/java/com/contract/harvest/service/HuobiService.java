package com.contract.harvest.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.dao.ContractOrderDAO;
import com.contract.harvest.entity.ContractOrderDO;
import com.contract.harvest.entity.HuobiEntity;
import com.huobi.common.request.Order;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PropertySource({"classpath:/exchange.properties"})
@Service
public class HuobiService {

    private static final Logger logger = LoggerFactory.getLogger(HuobiService.class);

    @Autowired
    private HuobiEntity huobiEntity;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ContractOrderDAO contractOrderDAO;

    @Value("${space.basis_percent}")
    private float basis_percent;

    @Value("${space.basis_close_percent}")
    private float basis_close_percent;

    @Value("${space.basis_percent_after}")
    private float basis_percent_after;

    @Value("${space.volume}")
    private String volume;

    @Value("${space.lever_rate}")
    private String lever_rate;

    @Value("${space.order_price_type}")
    private String order_price_type;

    @Autowired
    private VerifyParams verifyParams;

    public String invoke_bi_info(String symbol,String contract_type, String contract_code) {
        String bi_info = "";
        try{
            bi_info = huobiEntity.getContractInfo(symbol,contract_type,contract_code);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return bi_info;
    }

    public void invoke_bi(Map<String,String> params) throws Exception {
        //获取季度合约
        String symbol = params.get("symbol");
        Map<String, String> priceMap;
        Map<String, String> directionMap;
        priceMap = verifyParams.checkContractDeal(symbol);
        //基差
        Map<String, Float> contractAllPrice;
        contractAllPrice = verifyParams.getContractAllPrice(symbol,String.valueOf(priceMap.get("price_flag")));
        if (contractAllPrice.size() != 2) {
            throw new Exception("获取季度，周合约价格：basisAllArr.size():["+contractAllPrice+"]");
        }
        //季，周 的价格
        float contract_price_cq,contract_qcPrice;
        //季度
        contract_price_cq = contractAllPrice.get("quarterPrice");
        //周
        contract_qcPrice = contractAllPrice.get("qcPrice");

        //获取买卖方向
        directionMap = verifyParams.getBasisFlag(contract_price_cq,contract_qcPrice);
        //交易代码
        String cq_contract_type = priceMap.get("cq_contract_type");
        String nc_contract_type = priceMap.get("nc_contract_type");
        //开仓次数
        long position_num = Long.parseLong(priceMap.get("position_num"));
        //当前基差
        float now_percent = Float.parseFloat(directionMap.get("percent"));
        //开仓方向
        String quarter_direction = directionMap.get("quarter_direction");
        String week_direction = directionMap.get("week_direction");
        //上次开仓时的基差
        float pre_percent = cacheService.get_percent_flag(symbol);
//        logger.info("交易次数:"+position_num+"map:"+priceMap);
        //交易代码
        String[] contract_code_arr = String.valueOf(priceMap.get("contract_code")).split("-");
        if (contract_code_arr.length != 2)
        {
            throw new Exception("交易代码，contract_code_arr:["+Arrays.toString(contract_code_arr)+"]");
        }
        //如果没有仓位
        if (position_num == 0)
        {
            if (now_percent > basis_percent) {
                List<Order> orders = verifyParams.getListOrder(symbol,basis_percent,volume,"open",lever_rate,order_price_type,contract_code_arr,quarter_direction,week_direction,cq_contract_type,nc_contract_type);
                if (orders.isEmpty()) {
                    return;
                }
                //开仓下单
                String contractBatchorder = huobiEntity.futureContractBatchorder(orders);
                cacheService.order_insert_queue(symbol,"open",contractBatchorder);
                Thread.sleep(3000);
                //订阅通知
                cacheService.inform_sub("order_queue","handle_order");
                logger.info("开仓下单-币:"+symbol+"当前基差:"+directionMap.get("percent")+"order_info:"+orders.toString());
                return;
            }else {
                logger.info("不做处理,币:"+symbol+"当前基差:"+directionMap.get("percent")+"设置基差:"+basis_percent);
                return;
            }
        //或者开仓次数等于1
        }else if (position_num >= 1)
        {
            float flag_percent = now_percent - pre_percent;

            if (now_percent < basis_close_percent) {
                //获取平仓方向
                Map<String,String> flagDirection = verifyParams.getContractAccountPositionInfo(symbol,cq_contract_type,nc_contract_type);
                if (flagDirection.isEmpty()) {
                    logger.info("币："+symbol+"没有可平仓位"+"当前基差:"+directionMap.get("percent"));
                    cacheService.inform_sub("order_queue","handle_close_order");
                    return;
                }
                quarter_direction = flagDirection.get(cq_contract_type);
                week_direction = flagDirection.get(nc_contract_type);
                String sum_volume = flagDirection.get("volume");
                float profit = Float.parseFloat(flagDirection.get("profit"));
                if (profit < 0) {
                    logger.info("币:"+symbol+"亏损无法平仓-盈利:"+profit+"当前基差:"+directionMap.get("percent"));
                    return;
                }
                //平仓
                List<Order> orders = verifyParams.getListOrder(symbol,basis_close_percent,sum_volume,"close",lever_rate,order_price_type,contract_code_arr,quarter_direction,week_direction,cq_contract_type,nc_contract_type);
                if (orders.isEmpty()) {
                    return;
                }
                //平仓下单
                String contractBatchorder = huobiEntity.futureContractBatchorder(orders);
                cacheService.order_insert_queue(symbol,"close_all",contractBatchorder);
                Thread.sleep(3000);
                //订阅通知
                cacheService.inform_sub("order_queue","handle_close_order");
                //清仓
                logger.info("清仓下单,币:"+symbol+"预估盈利:"+profit+"平仓张数:"+sum_volume+"当前基差:"+directionMap.get("percent")+"清仓基差:"+basis_close_percent+"交易次数:"+position_num+"订单"+contractBatchorder);
                return;
            }
            /**
             * (加仓) = abs(当前基差 - 上次基差) > 第二次及之后开仓基差百分比 && (当前基差 - 上次基差) > 0
             * (减仓) = abs(当前基差 - 上次基差) > 第二次及之后开仓基差百分比 && (当前基差 - 上次基差) < 0
             * (清仓) = 当前基差 < 交易平仓百分比
             */
            if (Math.abs(flag_percent) > basis_percent_after) {
                if (flag_percent > 0)
                {
                    //加仓
                    List<Order> orders = verifyParams.getListOrder(symbol,basis_percent_after,volume,"open",lever_rate,order_price_type,contract_code_arr,quarter_direction,week_direction,cq_contract_type,nc_contract_type);
                    if (orders.isEmpty()) {
                        return;
                    }
                    //开仓下单
                    String contractBatchorder = huobiEntity.futureContractBatchorder(orders);
                    cacheService.order_insert_queue(symbol,"open",contractBatchorder);
                    Thread.sleep(3000);
                    //订阅通知
                    cacheService.inform_sub("order_queue","handle_order");
                    logger.info("加仓下单-币:"+symbol+"当前基差:"+directionMap.get("percent")+"order_info:"+orders.toString());
                    return;
                }else if (flag_percent < 0) {
                    //获取平仓方向
                    Map<String,String> flagDirection = verifyParams.getContractAccountPositionInfo(symbol,cq_contract_type,nc_contract_type);
                    if (flagDirection.isEmpty()) {
                        logger.info("币：" + symbol + "没有可减仓位"+"当前基差:"+directionMap.get("percent"));
                        return;
                    }
                    quarter_direction = flagDirection.get(cq_contract_type);
                    week_direction = flagDirection.get(nc_contract_type);
                    float profit = Float.parseFloat(flagDirection.get("profit"));
                    //减仓
                    List<Order> orders = verifyParams.getListOrder(symbol,basis_percent_after,volume,"close",lever_rate,order_price_type,contract_code_arr,quarter_direction,week_direction,cq_contract_type,nc_contract_type);
                    if (profit < 0) {
                        logger.info("币:"+symbol+"亏损无法减仓-盈利:"+profit+"当前基差:"+directionMap.get("percent"));
                        return;
                    }
                    //减仓下单
                    String contractBatchorder = huobiEntity.futureContractBatchorder(orders);
                    cacheService.order_insert_queue(symbol,"close",contractBatchorder);
                    Thread.sleep(1000);
                    //订阅通知
                    cacheService.inform_sub("order_queue","handle_close_order");
//                    this.handleCloseOrder();
                    logger.info("减仓下单,币:"+symbol+"预估盈利:"+profit+"当前基差:"+directionMap.get("percent")+"上次基差"+verifyParams.getFloatNum(pre_percent)+"订单"+orders.toString());
                    return;
                }else {
                    logger.info("不做处理--次数:"+position_num+symbol+"当前基差:"+directionMap.get("percent"));
                    return;
                }
            }
            logger.info("次数:"+position_num+symbol+"当前基差:"+directionMap.get("percent"));
        }
        return;
    }
    /**
     * 获取订单实体
     */
    public List<ContractOrderDO> getContractOrderDO(JSONArray orderData,String flag) throws Exception  {
        List<ContractOrderDO> orderDoList = new ArrayList<>();
        for (Object order:orderData) {
            JSONObject jo_order = (JSONObject)order;
            //获取实体
            ContractOrderDO orderDo = contractOrderDAO.getOrderDO(jo_order,flag);
            orderDoList.add(orderDo);
        }
        return orderDoList;
    }

    /**
     * 处理队列中的订单
     */
    public void handleOrder() throws Exception {
        //获取队列里的一个订单
        String order_info_str = cacheService.get_order_in_queue("open");
//        order_info_str = "{\"symbol\":\"BSV\",\"status\":\"ok\",\"data\":{\"errors\":[],\"success\":[{\"order_id\":709565299313250304,\"index\":1,\"order_id_str\":\"709565299313250304\"},{\"order_id\":709565299363581952,\"index\":2,\"order_id_str\":\"709565299363581952\"}]},\"ts\":1587125375356}";
        if (StringUtils.isEmpty(order_info_str)) {
            logger.info("获取队列中的订单为空:["+order_info_str+"]");
            return;
        }
        //获取订单id
        Map<String,String> orderMap = verifyParams.getOrderIdStr(order_info_str);
        String order_id_str = orderMap.get("order_id_str");
        String symbol = orderMap.get("symbol");
        if (order_id_str.equals("") || symbol.equals("")) {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("open");
            logger.error("队列中的订单:[币:"+symbol+"订单信息:"+order_info_str+"]");
            return;
        }
        //检测订单是否存在set中
        Boolean orderIdExistsFlag = cacheService.getOrderIdStrIsExist(order_id_str);
        if (!orderIdExistsFlag) {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("close");
            logger.error("订单id已存在:[币:"+symbol+"订单id:"+order_id_str+"]");
            return;
        }
        //获取订单信息
        String contractOrderInfo = huobiEntity.getcontractOrderInfo(order_id_str,"",symbol);
        //将已成功成交的订单存入队列与数据库
        JSONArray orderData = JSONObject.parseObject(contractOrderInfo).getJSONArray("data");
        if (orderData == null || orderData.size() != 2)
        {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("open");
            logger.error("队列中的订单信息:[orderData.size() != 2"+contractOrderInfo+"]");
            return;
        }
        String contract_code_str = orderData.getJSONObject(0).getString("contract_code")+"-"+orderData.getJSONObject(1).getString("contract_code");
        //获取实体
        List<ContractOrderDO> orderDoList = this.getContractOrderDO(orderData,"insert");
        //成交均价
        float cq_price = orderData.getJSONObject(0).getFloatValue("trade_avg_price");
        float nc_price = orderData.getJSONObject(1).getFloatValue("trade_avg_price");
        if (cq_price <= 0 || nc_price <= 0) {
            cq_price = orderData.getJSONObject(0).getFloatValue("price");
            nc_price = orderData.getJSONObject(1).getFloatValue("price");
        }
        //记录本次下单基差
        Map<String,String> basisPriceMap = verifyParams.getBasisFlag(cq_price,nc_price);
        float sure_basis_percent = Float.parseFloat(basisPriceMap.get("percent"));
        if (sure_basis_percent == 0)
        {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("open");
            logger.error("成交均价基差为0:["+contractOrderInfo+"]");
        }
        //设置基差
        cacheService.set_percent_flag(symbol,verifyParams.getFloatNum(sure_basis_percent));
        //设置本次交易的币种周期
        cacheService.set_redis_key_deal_flag(contract_code_str,orderMap.get("symbol"));
        //订单成交信息入队列
        cacheService.deal_order_queue(contract_code_str,JSONArray.toJSONString(orderData),orderMap.get("symbol"));
        //订单成交信息入数据库
        Long num = contractOrderDAO.insertBatch(orderDoList);
        //移除本条挂单记录
        cacheService.lpop_order_in_queue("open");
        logger.info("币:"+symbol+"开仓下单信息:[成交订单数量"+num+"基差:"+verifyParams.getFloatNum(sure_basis_percent)+"季度:"+cq_price+"周:"+nc_price+"订单信息:"+contractOrderInfo+"]");
    }

    /**
     * 处理平仓订单
     */
    public void handleCloseOrder() throws Exception {
        //获取队列里的平仓一个订单
        String order_info_str = cacheService.get_order_in_queue("close");
//            order_info_str = "{\"symbol\":\"ETC\",\"data\":{\"success\":[{\"order_id_str\":\"709131828400963584\",\"index\":1,\"order_id\":709131828400963584},{\"order_id_str\":\"709131828426129409\",\"index\":2,\"order_id\":709131828426129409}],\"errors\":[]},\"offset\":\"close_all\",\"status\":\"ok\",\"ts\":1587484639937}";
        if (StringUtils.isEmpty(order_info_str)) {
            logger.error("获取平仓队列中的订单为空:["+order_info_str+"]");
            return;
        }
        //获取订单id
        Map<String,String> orderMap = verifyParams.getOrderIdStr(order_info_str);
        if (orderMap.isEmpty()) {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("close");
            logger.error("平仓队列中的订单信息有误:["+order_info_str+"]");
            return;
        }
        String order_id_str = orderMap.get("order_id_str");
        String symbol = orderMap.get("symbol");
        String offset = orderMap.get("offset");
        if (order_id_str.equals("") || symbol.equals("")) {
            //从挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("close");
            logger.error("平仓队列中的订单:[币:"+symbol+"订单信息:"+order_info_str+"]");
            return;
        }
        //检测订单是否存在set中
        Boolean orderIdExistsFlag = cacheService.getOrderIdStrIsExist(order_id_str);
//        if (!orderIdExistsFlag) {
//            //从挂单队列中将订单取出并放到错误队列
//            cacheService.lpop_order_to_error_queue("close");
//            logger.error("订单id已存在:[币:"+symbol+"订单id:"+order_id_str+"]");
//            return;
//        }
        //获取订单信息
        String contractOrderInfo = huobiEntity.getcontractOrderInfo(order_id_str,"",symbol);
        //将已成功平仓的订单修改数据库
        JSONArray orderData = JSONObject.parseObject(contractOrderInfo).getJSONArray("data");
        if (orderData == null || orderData.size() != 2)
        {
            //从平仓挂单队列中将订单取出并放到错误队列
            cacheService.lpop_order_to_error_queue("close");
            logger.error("平仓队列中的订单信息:[orderData.size() != 2"+contractOrderInfo+"]");
            return;
        }
        String contract_code_str = orderData.getJSONObject(0).getString("contract_code")+"-"+orderData.getJSONObject(1).getString("contract_code");
        //移除本条平仓记录
        cacheService.lpop_order_in_queue("close");
        //订单成交信息移除队列
        cacheService.remove_deal_order_queue(contract_code_str,offset);
        //移除基差
        cacheService.remove_percent_flag(symbol,offset);
        //移除交易的币种周期
        if (offset.equals("close_all")) {
            cacheService.del_redis_key_deal_flag(symbol);
        }
        //修改订单信息
        //获取实体
        List<ContractOrderDO> orderDoList = this.getContractOrderDO(orderData,"insert");

        ContractOrderDO one = orderDoList.get(0);
        ContractOrderDO two = orderDoList.get(1);
        //收益
        String sumProfit = verifyParams.getFloatNum(one.getProfit() + two.getProfit());
        //手续费
        String sumFee = verifyParams.getFloatNum(one.getFee() + two.getFee());
        //基差
        String basisPrice = verifyParams.getFloatNum(Math.abs(one.getTradeAvgPrice() - two.getTradeAvgPrice()) / one.getTradeAvgPrice() * 100);
        //订单成交信息入数据库
        Long num = contractOrderDAO.insertBatch(orderDoList);
        logger.info("币:"+symbol+"平仓下单信息:[收益"+sumProfit+"手续费"+sumFee+"基差:"+basisPrice+"%订单信息:"+contractOrderInfo+"]");
//        Iterator<ContractOrderDO> iterator = orderDoList.iterator();
//        while (iterator.hasNext()) {
//            contractOrderDAO.updateByOrderId(iterator.next());
//        }
    }

}
