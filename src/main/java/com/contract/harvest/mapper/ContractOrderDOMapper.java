package com.contract.harvest.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import com.contract.harvest.entity.ContractOrderDO;
import java.util.List;

/**
 * 由于需要对分页支持,请直接使用对应的DAO类
 * The Table contract_order.
 * 交易订单表
 */
@Repository
@Mapper
public interface ContractOrderDOMapper {

    /**
     * desc:插入表:contract_order.<br/>
     * descSql =  SELECT LAST_INSERT_ID() <![CDATA[ INSERT INTO contract_order( ID ,FEE ,Z_TS ,PROFIT ,TRADE_VOLUME ,TRADE_AVG_PRICE ,TRADE_TURNOVER ,PRICE ,ORDER_ID ,Z_OFFSET ,DIRECTION ,CONTRACT_TYPE ,ORDER_PRICE_TYPE ,ORDER_TYPE ,VOLUME ,LEVER_RATE ,ORDER_STATUS ,CREATE_TIME ,UPDATE_TIME )VALUES( null , #{fee,jdbcType=DECIMAL} , #{zTs,jdbcType=BIGINT} , #{profit,jdbcType=DECIMAL} , #{tradeVolume,jdbcType=DECIMAL} , #{tradeAvgPrice,jdbcType=DECIMAL} , #{tradeTurnover,jdbcType=DECIMAL} , #{price,jdbcType=DOUBLE} , #{orderId,jdbcType=VARCHAR} , #{zOffset,jdbcType=CHAR} , #{direction,jdbcType=CHAR} , #{contractType,jdbcType=VARCHAR} , #{orderPriceType,jdbcType=VARCHAR} , #{orderType,jdbcType=BIT} , #{volume,jdbcType=INTEGER} , #{leverRate,jdbcType=TINYINT} , #{orderStatus,jdbcType=TINYINT} , #{createTime,jdbcType=TIMESTAMP} , #{updateTime,jdbcType=TIMESTAMP} ) ]]>
     * @param entity entity
     * @return Long
     */
    Long insert(ContractOrderDO entity);
    /**
     * desc:批量插入表:contract_order.<br/>
     * descSql =  INSERT INTO contract_order( ID ,FEE ,Z_TS ,PROFIT ,TRADE_VOLUME ,TRADE_AVG_PRICE ,TRADE_TURNOVER ,PRICE ,ORDER_ID ,Z_OFFSET ,DIRECTION ,CONTRACT_TYPE ,ORDER_PRICE_TYPE ,ORDER_TYPE ,VOLUME ,LEVER_RATE ,ORDER_STATUS ,CREATE_TIME ,UPDATE_TIME )VALUES ( null , #{item.fee,jdbcType=DECIMAL} , #{item.zTs,jdbcType=BIGINT} , #{item.profit,jdbcType=DECIMAL} , #{item.tradeVolume,jdbcType=DECIMAL} , #{item.tradeAvgPrice,jdbcType=DECIMAL} , #{item.tradeTurnover,jdbcType=DECIMAL} , #{item.price,jdbcType=DOUBLE} , #{item.orderId,jdbcType=VARCHAR} , #{item.zOffset,jdbcType=CHAR} , #{item.direction,jdbcType=CHAR} , #{item.contractType,jdbcType=VARCHAR} , #{item.orderPriceType,jdbcType=VARCHAR} , #{item.orderType,jdbcType=BIT} , #{item.volume,jdbcType=INTEGER} , #{item.leverRate,jdbcType=TINYINT} , #{item.orderStatus,jdbcType=TINYINT} , #{item.createTime,jdbcType=TIMESTAMP} , #{item.updateTime,jdbcType=TIMESTAMP} ) 
     * @param list list
     * @return Long
     */
    Long insertBatch(List<ContractOrderDO> list);
    /**
     * desc:根据主键删除数据:contract_order.<br/>
     * descSql =  <![CDATA[ DELETE FROM contract_order WHERE ID = #{id,jdbcType=INTEGER} ]]>
     * @param id id
     * @return Long
     */
    Long deleteById(Integer id);
    /**
     * desc:根据主键获取数据:contract_order.<br/>
     * descSql =  SELECT * FROM contract_order WHERE <![CDATA[ ID = #{id,jdbcType=INTEGER} ]]>
     * @param id id
     * @return ContractOrderDO
     */
    ContractOrderDO getById(Integer id);

    /**
     * 根据订单ID修改数据
     */
    @Update("update contract_order set update_time=#{update_time},order_status=#{orderStatus},order_type=#{orderType},profit=#{profit},fee=#{fee} where order_id=#{orderId}")
    Long updateByOrderId(String update_time, String orderId,Integer orderStatus,Integer orderType,Double profit,Double fee);
}
