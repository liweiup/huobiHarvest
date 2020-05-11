package com.contract.harvest.dao;

import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.entity.ContractOrderDO;
import com.contract.harvest.mapper.ContractOrderDOMapper;
import com.contract.harvest.service.VerifyParams;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Timer;

/**
* The Table contract_order.
* 交易订单表
*/
@Repository
public class ContractOrderDAO {

    @Autowired
    private ContractOrderDOMapper contractOrderDOMapper;
    @Autowired
    private VerifyParams verifyParams;

    /**
     * desc:插入表:contract_order.<br/>
     *
     * @param entity entity
     * @return Long
     */
    public Long insert(ContractOrderDO entity) {
        return contractOrderDOMapper.insert(entity);
    }

    /**
     * desc:批量插入表:contract_order.<br/>
     *
     * @param list list
     * @return Long
     */
    public Long insertBatch(List<ContractOrderDO> list) {
        return contractOrderDOMapper.insertBatch(list);
    }

    /**
     * desc:根据主键删除数据:contract_order.<br/>
     *
     * @param id id
     * @return Long
     */
    public Long deleteById(Integer id) {
        return contractOrderDOMapper.deleteById(id);
    }

    /**
     * desc:根据主键获取数据:contract_order.<br/>
     *
     * @param id id
     * @return ContractOrderDO
     */
    public ContractOrderDO getById(Integer id) {
        return contractOrderDOMapper.getById(id);
    }

    /**
     * 根据订单ID修改数据
     */
    public Long updateByOrderId(ContractOrderDO entity){
        String update_time = entity.getUpdateTime();
        String orderId = entity.getOrderId();
        Integer orderStatus = entity.getOrderStatus();
        Integer orderType = entity.getOrderType();
        Double profit = entity.getProfit();
        Double fee = entity.getFee();
        return contractOrderDOMapper.updateByOrderId(update_time,orderId,orderStatus,orderType,profit,fee);
    }
    /**
     * 将订单信息转为对象
     */
    public ContractOrderDO getOrderDO(JSONObject order,String flag) {
        ContractOrderDO orderDO = new ContractOrderDO();
        if (StringUtils.isNotEmpty(order.getString("symbol"))) {
            orderDO.setSymbol(order.getString("symbol"));
        }
        if (StringUtils.isNotEmpty(order.getString("lever_rate"))) {
            orderDO.setLeverRate(order.getIntValue("lever_rate"));
        }
        if (StringUtils.isNotEmpty(order.getString("order_id"))) {
            orderDO.setOrderId(order.getString("order_id"));
        }
        if (StringUtils.isNotEmpty(order.getString("trade_volume"))) {
            orderDO.setTradeVolume(order.getLongValue("trade_volume"));
        }
        if (StringUtils.isNotEmpty(order.getString("offset"))) {
            orderDO.setZOffset(order.getString("offset"));
        }
        if (!Double.isNaN(order.getDoubleValue("trade_turnover"))) {
            orderDO.setTradeTurnover(order.getDoubleValue("trade_turnover"));
        }
        if (!Double.isNaN(order.getDoubleValue("fee"))) {
            orderDO.setFee(order.getDoubleValue("fee"));
        }
        if (StringUtils.isNotEmpty(order.getString("created_at"))) {
            orderDO.setZTs(order.getLongValue("created_at"));
        }
        if (StringUtils.isNotEmpty(order.getString("order_price_type"))) {
            orderDO.setOrderPriceType(order.getString("order_price_type"));
        }
        if (StringUtils.isNotEmpty(order.getString("volume"))) {
            orderDO.setVolume(Integer.parseInt(order.getString("volume")));
        }
        if (StringUtils.isNotEmpty(order.getString("contract_type"))) {
            orderDO.setContractType(order.getString("contract_type"));
        }
        if (StringUtils.isNotEmpty(order.getString("contract_code"))) {
            orderDO.setContract_code(order.getString("contract_code"));
        }
        if (!Double.isNaN(order.getDoubleValue("price"))) {
            orderDO.setPrice(order.getDoubleValue("price"));
        }
        if (StringUtils.isNotEmpty(order.getString("order_type"))) {
            orderDO.setOrderType(order.getIntValue("order_type"));
        }
        if (!Double.isNaN(order.getDoubleValue("profit"))) {
            orderDO.setProfit(order.getDoubleValue("profit"));
        }
        if (StringUtils.isNotEmpty(order.getString("direction"))) {
            orderDO.setDirection(order.getString("direction"));
        }
        if (StringUtils.isNotEmpty(order.getString("status"))) {
            orderDO.setOrderStatus(order.getIntValue("status"));
        }
        //成交均价
        if (!Double.isNaN(order.getDoubleValue("trade_avg_price"))) {
            orderDO.setTradeAvgPrice(order.getDoubleValue("trade_avg_price"));
        }
        String nowTime = verifyParams.getNowDatetime();
        switch (flag) {
            case "insert":
                orderDO.setCreateTime(nowTime);
                break;
            case "update":
                orderDO.setUpdateTime(nowTime);
                break;
        }
        return orderDO;
    }
}
