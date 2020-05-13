package com.contract.harvest.entity;

import com.huobi.common.api.IHbdmRestApi;
import com.huobi.common.request.Order;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@CacheConfig(cacheNames="huobi:api:cache")
public class HuobiEntity {

    @Autowired
    private IHbdmRestApi futureGetV1;

    @Autowired
    private IHbdmRestApi futurePostV1;

    //获取合约信息
    @Cacheable(keyGenerator = "HuobiEntity_keyGenerator",cacheManager = "huobiEntityRedisCacheManager")
    public String getContractInfo(String symbol,String contract_type, String contract_code) throws HttpException,IOException{
        String contractInfo = "";
        contractInfo = futureGetV1.futureContractInfo(symbol, contract_type, contract_code);
        return contractInfo;
    }

    //获取合约指数信息
    public String getContractIndex(String symbol) throws IOException, HttpException {
        String contractIndex = "";
        contractIndex = futureGetV1.futureContractIndex(symbol);
        return contractIndex;
    }
    //获取合约最高限价和最低限价
    public String getPriceLimit(String symbol,String contract_type, String contract_code) throws IOException, HttpException {
        String priceLimit = "";
        priceLimit = futureGetV1.futurePriceLimit(symbol, contract_type, contract_code);
        return priceLimit;
    }
    //获取当前可用合约总持仓量
    public String getOpenInterest(String symbol,String contract_type, String contract_code) throws IOException, HttpException {
        String openInterest = "";
        openInterest = futureGetV1.futureOpenInterest(symbol, contract_type, contract_code);
        return openInterest;
    }
    /**
     * 获取行情深度数据
     * symbol	string	true	如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * type	string	true	获得150档深度数据，使用step0, step1, step2, step3, step4, step5（step1至step5是进行了深度合并后的深度），
     * 使用step0时，不合并深度获取150档数据;获得20档深度数据，使用 step6, step7, step8, step9, step10, step11（step7至step11是进行了深度合并后的深度），
     * 使用step6时，不合并深度获取20档数据
     */
    public String getMarketDepth(String symbol,String type) throws IOException, HttpException {
        String marketDepth = "";
        marketDepth = futureGetV1.futureMarketDepth(symbol,type);
        return marketDepth;
    }

    /**
     * 获取K线数据
     * symbol	true	string	合约名称		如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * period	true	string	K线类型		1min, 5min, 15min, 30min, 60min,4hour,1day, 1mon
     * size	true	integer	获取数量	150	[1,2000]
     * from	false	integer	开始时间戳 10位 单位S
     * to	false	integer	结束时间戳 10位 单位S
     */
    public String getMarketHistoryKline(String symbol,String period,String size) throws IOException, HttpException {
        String marketHistoryKline = "";
        marketHistoryKline = futureGetV1.futureMarketHistoryKline(symbol,period,size);
        return marketHistoryKline;
    }
    /**
     * 获取聚合行情
     */
    public String getMarketDetailMerged(String symbol) throws IOException, HttpException {
        String marketDetailMerged = "";
        marketDetailMerged = futureGetV1.futureMarketDetailMerged(symbol);
        return marketDetailMerged;
    }
    /**
     * 批量获取最近的交易记录
     * symbol	true	string	合约名称		如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * size	true	number	获取交易记录的数量	1	[1, 2000]
     */
    public String getMarketHistoryTrade(String symbol,String size) throws IOException, HttpException {
        String marketDetailTrade = "";
        marketDetailTrade = futurePostV1.futureMarketHistoryTrade(symbol, size);
        return marketDetailTrade;
    }
    /**
     * 获取用户账户信息
     * @param symbol string	品种代码 "BTC","ETH"...如果缺省，默认返回所有品种
     */
    public String getContractAccountInfo(String symbol) throws IOException, HttpException {
        String accountInfo = "";
        accountInfo = futurePostV1.futureContractAccountInfo(symbol);
        return accountInfo;
    }

    /**
     * 获取用户持仓信息
     * @param symbol
     */
    public String getContractPositionInfo(String symbol) throws IOException, HttpException {
        String positionInfo = "";
        positionInfo = futurePostV1.futureContractPositionInfo(symbol);
        return positionInfo;
    }

    /**
     * 查询用户账户和持仓信息
     * post api/v1/contract_account_position_info
     */
    public String getContractAccountPositionInfo(String symbol) throws IOException, HttpException {
        String positionInfo = "";
        positionInfo = futurePostV1.futureContractAccountPositionInfo(symbol);
        return positionInfo;
    }


    /*************************************合约下单************************************************************/

    /**
     * 合约下单
     * 参数名	参数类型	必填	描述
     * symbol	string	true	"BTC","ETH"...
     * contract_type	string	true	合约类型 ("this_week":当周 "next_week":下周 "quarter":季度)
     * contract_code	string	true	BTC180914
     * client_order_id	long	false	客户自己填写和维护，必须为数字
     * price	decimal	false	价格
     * volume	long	true	委托数量(张)
     * direction	string	true	"buy":买 "sell":卖
     * offset	string	true	"open":开 "close":平
     * lever_rate	int	true	杠杆倍数[“开仓”若有10倍多单，就不能再下20倍多单]
     * order_price_type	string	true    订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,
       post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，
       fok：FOK订单, "opponent_ioc"： 对手价-IOC下单，"optimal_5_ioc"：最优5档-IOC下单，"optimal_10_ioc"：最优10档-IOC下单，
       "optimal_20_ioc"：最优20档-IOC下单,"opponent_fok"： 对手价-FOK下单，"optimal_5_fok"：最优5档-FOK下单，"optimal_10_fok"：
        最优10档-FOK下单，"optimal_20_fok"：最优20档-FOK下单
        备注：
        如果contract_code填了值，那就按照contract_code去下单，如果contract_code没有填值，则按照symbol+contract_type去下单。
        对手价下单price价格参数不用传，对手价下单价格是买一和卖一价,optimal_5：最优5档、optimal_10：
        最优10档、optimal_20：最优20档下单price价格参数不用传，"limit":限价，"post_only":只做maker单 需要传价格，"fok"：全部成交或立即取消，"ioc":立即成交并取消剩余。
        Post only(也叫maker only订单，只下maker单)每个周期合约的开仓/平仓的下单数量限制为500000，同时也会受到用户持仓数量限制。
        开平方向
        开多：买入开多(direction用buy、offset用open)
        平多：卖出平多(direction用sell、offset用close)
        开空：卖出开空(direction用sell、offset用open)
        平空：买入平空(direction用buy、offset用close)
     */
    public String futureContractOrder(String symbol,String contract_type,String contract_code,String client_order_id,String price,String volume,String direction,String offset,String lever_rate,String order_price_type) throws IOException, HttpException {
        String contractOrder = "";
        contractOrder = futurePostV1.futureContractOrder(symbol, contract_type, contract_code, client_order_id, price, volume, direction, offset, lever_rate, order_price_type);
        return contractOrder;
    }

    /**
     * 批量下单
     * List<Order> orders = new ArrayList();
     * Order order1 = new Order("BTC", "this_week", "BTC181110", "10", "6400", "1", "buy", "open", "10", "limit");
     */
    public String futureContractBatchorder(List<Order> orders) throws IOException, HttpException {
        String contractBatchorder = "";
        contractBatchorder = futurePostV1.futureContractBatchorder(orders);
        return contractBatchorder;
    }
    /**
     * 合约取消订单
     * POST api/v1/contract_cancel
     * 参数名称	是否必须	类型	描述
     * order_id	false	string	订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     * client_order_id	false	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     * symbol	true	string	"BTC","ETH"...
     */
    public String futureContractCancel(String order_id,String client_order_id,String symbol) throws IOException, HttpException {
        String contractcancel = "";
        contractcancel = futurePostV1.futureContractCancel(order_id, client_order_id, symbol);
        return contractcancel;
    }
    /**
     * 合约全部撤单
     * POST api/v1/contract_cancelall
     * 请求参数
     * 参数名称	是否必须	类型	描述
     * symbol	true	string	品种代码，如"BTC","ETH"...
     * contract_code	false	string	合约code
     * contract_type	false	string	合约类型
     */
    public String futureContractCancelall(String symbol) throws IOException, HttpException {
        String contractCancelall = "";
        contractCancelall = futurePostV1.futureContractCancelall(symbol);
        return contractCancelall;
    }

    /**
     * 获取合约订单信息
     * 参数名称 是否必须    类型	描述
     * order_id	请看备注	string	订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * client_order_id	请看备注	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * symbol	true	string	"BTC","ETH"...
     */
    public String getcontractOrderInfo(String order_id,String client_order_id,String symbol) throws IOException, HttpException {
        String contractOrderInfo = "";
        contractOrderInfo = futurePostV1.futureContractOrderInfo(order_id,client_order_id,symbol,"");
        return contractOrderInfo;
    }
    /**
     * 获取订单明细信息
     * POST api/v1/contract_order_detail
     * 请求参数
     * 参数名称	是否必须	类型	描述
     * symbol	true	string	"BTC","ETH"...
     * order_id	true	bigint	订单id
     * created_at	false	long	下单时间戳
     * order_type	false	int	订单类型，1:报单 、 2:撤单 、 3:强平、4:交割
     * page_index	false	int	第几页,不填第一页
     * page_size	false	int	不填默认20，不得多于50
     */
    public String getContractOrderDetail(String symbol,String order_id,String created_at,String order_type,String page_index,String page_size) throws IOException, HttpException {
     String orderDetail = "";
     orderDetail = futurePostV1.futureContractOrderDetail(symbol,order_id,created_at,order_type,page_index,page_size);
     return orderDetail;
    }

    /**
     * 获取合约当前未成交委托
     * POST api/v1/contract_openorders
     * 请求参数
     * 参数名称	是否必须	类型	描述	默认值	取值范围
     * symbol	true	string	品种代码		"BTC","ETH"...
     * page_index	false	int	页码，不填默认第1页	1
     * page_size	false	int			不填默认20，不得多于50
     */
    public String getContractOpenorders(String symbol,String page_index,String page_size) throws IOException, HttpException {
        String openorders = "";
        openorders = futurePostV1.futureContractOpenorders(symbol,page_index,page_size);
        return openorders;
    }

    /**
     * 获取合约历史委托
     * 参数名称	是否必须	类型	描述	默认值	取值范围
     * symbol	true	string	品种代码		"BTC","ETH"...
     * trade_type	true	int	交易类型		0:全部,1:买入开多,2: 卖出开空,3: 买入平空,4: 卖出平多,5: 卖出强平,6: 买入强平,7:交割平多,8: 交割平空, 11:减仓平多，12:减仓平空
     * type	true	int	类型		1:所有订单,2:结束状态的订单
     * status	true	int	订单状态		0:全部,3:未成交, 4: 部分成交,5: 部分成交已撤单,6: 全部成交,7:已撤单
     * create_date	true	int	日期		可随意输入正整数, ，如果参数超过90则默认查询90天的数据
     * page_index	false	int		页码，不填默认第1页	1
     * page_size	false	int	每页条数，不填默认20	20	不得多于50
     * contract_code	false	string	合约代码
     * order_type	false	string	订单类型		1：限价单、3：对手价、4：闪电平仓、5：计划委托、6：post_only、7：最优5档、8：最优10档、9：最优20档、10：fok、11：ioc
     */
    public String getContractHisorders(String symbol,String trade_type,String type,String status,String create_date,String page_index,String page_size) throws IOException, HttpException {
        String hisorders = "";
        hisorders = futurePostV1.futureContractHisorders(symbol,trade_type,type,status,create_date,page_index,page_size);
        return hisorders;
    }

    /**
     *
     * 精英账户多空持仓对比-账户数
     * 实例
     * GET api/v1/contract_elite_account_ratio
     * curl "https://api.hbdm.com/api/v1/contract_elite_account_ratio?symbol=BTC&period=60min"
     * 请求参数
     * 参数名称	是否必须	类型	描述	取值范围
     * symbol	true	string	品种代码	"BTC","ETH"...
     * period	true	string	周期	5min, 15min, 30min, 60min,4hour,1day
     */
    public String getContractEliteAccountRatio(String symbol, String period) throws IOException, HttpException {
        String hisbasisAll = "";
        hisbasisAll = futurePostV1.futureContractEliteAccountRatio(symbol,period);
        return hisbasisAll;
    }

    /**
     *
     * 精英账户多空持仓对比-持仓量
     * 实例
     * GET api/v1/contract_elite_position_ratio
     * curl "https://api.hbdm.com/api/v1/contract_elite_position_ratio?symbol=BTC&period=60min"
     * 请求参数
     * 参数名称	是否必须	类型	描述	取值范围
     * symbol	true	string	品种代码	"BTC","ETH"...
     * period	true	string	周期	5min, 15min, 30min, 60min,4hour,1day
     */
    public String getContractElitePositionRatio(String symbol, String period) throws IOException, HttpException {
        String hisbasisAll = "";
        hisbasisAll = futurePostV1.futureContractElitePositionRatio(symbol,period);
        return hisbasisAll;
    }

    /**
     * 获取基差
     * symbol	true	string	合约名称		如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * period	true	string	周期		1min,5min, 15min, 30min, 60min,4hour,1day,1mon
     * basis_price_type	false	string	基差价格类型，表示在周期内计算基差使用的价格类型	不填，默认使用开盘价	开盘价：open，收盘价：close，最高价：high，最低价：low，平均价=（最高价+最低价）/2：average
     * size	true	int	基差获取数量	150	[1,2000]
     */
    public String getContractHisbasis(String symbol, String period, String basis_price_type, String size) throws IOException, HttpException {
        String hisbasis = "";
        hisbasis = futurePostV1.futureContractHisbasis(symbol,period,basis_price_type,size);
        return hisbasis;
    }

    public String getContractHisbasisAll(String symbol, String period, String basis_price_type, String size) throws IOException, HttpException {
        String hisbasisAll = "";
        hisbasisAll = futurePostV1.futureContractHisbasisAll(symbol,period,basis_price_type,size);
        return hisbasisAll;
    }
}
