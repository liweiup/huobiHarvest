package com.contract.harvest.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 由于Java的简单类型不能够精确的对浮点数进行运算，这个工具类提供精
 * 确的浮点数运算，包括加减乘除和四舍五入。
 */
public class Arith{ //默认除法运算精度
    private static final int DEF_DIV_SCALE = 10; //这个类不能实例化
    private Arith(){
    }
    /**
     * 提供精确的加法运算。
     * @param v1 被加数
     * @param v2 加数
     * @return 两个参数的和
     */
    public static double add(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2).doubleValue();
    }
    /**
     * 提供精确的减法运算。
     * @param v1 减数
     * @param v2 被减数
     * @return 两个参数的差
     */
    public static double sub(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2).doubleValue();
    }
    /**
     * 提供精确的乘法运算。
     * @param v1 被乘数
     * @param v2 乘数
     * @return 两个参数的积
     */
    public static double mul(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2).doubleValue();
    }
    /**
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到
     * 小数点以后10位，以后的数字四舍五入。
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(double v1,double v2){
        return div(v1,v2,DEF_DIV_SCALE);
    }
    /**
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指
     * 定精度，以后的数字四舍五入。
     * @param v1 除数
     * @param v2 被除数
     * @param scale 表示表示需要精确到小数点以后几位。
     * @return 两个参数的商
     */
    public static double div(double v1,double v2,int scale){
        if(scale<0){
            throw new IllegalArgumentException(
                    "The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2,scale, RoundingMode.UP).doubleValue();
    }
    /**
     * 提供精确的小数位四舍五入处理。
     * @param v 需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(double v,int scale){
        if(scale<0){
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        return b.setScale(scale,RoundingMode.UP).doubleValue();
    }
    /**
     * 比较大小
     */
    public static boolean compareNum(double v1,double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.compareTo(b2) >= 0;
    }

    public static boolean compareNum(int b1, int b2) {
        return b1 < b2;
    }

    /**
     * 科学计数法去除
     */
    public static String getStrBigDecimal(double d) {
        return BigDecimal.valueOf(d).toString();
    }

    /**
     * 开平仓盈亏
     * 多仓已实现盈亏=（合约面值 / 开仓均价 - 合约面值 / 平仓价格）* 平仓张数
     * 空仓已实现盈亏=（合约面值 / 平仓价格 - 合约面值 / 开仓均价）* 平仓张数
     */
    public static double arithProfit(int denomination,double openPrice,double closePrice,int volume,String direction) {
        if (direction.equals("long")) {
            return mul(sub(div(denomination,openPrice),div(denomination,closePrice)),volume);
        }else {
            return mul(sub(div(denomination,closePrice),div(denomination,openPrice)),volume);
        }
    }
    /**
     * 计算基差百分比
     */
    public static double arithBasisPercent(double priceOne,double priceTwo) {
        return div(sub(priceOne,priceTwo),priceOne);
    }
    /**
     * 手续费
     * @param denomination 面值
     * @param feePercent 百分比
     * @param volume 张数
     * @param volume 张数
     */
    public static double arithFee(double denomination,int volume,double price,double feePercent) {
        return mul(div(mul(denomination,volume),price),div(feePercent,100));
    }
}