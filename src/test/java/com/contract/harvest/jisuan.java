package com.contract.harvest;

public class jisuan {

    private static Integer position_num = 0;
    private static float basis_close_percent = (float) 0.0065;
    private static float basis_percent_after = (float) 0.004;

    public static void main(String[] args) {
        get_per((float) 0,(float) 0.065);//1
        get_per((float) 0.0066,(float) 0.0066);//1
        get_per((float) 0.0066,(float) 0.011);//1
        get_per((float) 0.011,(float) 0.0066);//1


    }

    public static void get_per(float pre_percent, float now_percent) {
        /**
         * 减仓 or 加仓
         * (加仓) = （当前基差 - 上次基差）> 第二次及之后开仓基差百分比
         * (减仓) = （当前基差 - 上次基差）> 第二次及之后开仓基差百分比
         *
         */
//        System.out.println("==="+Math.abs(now_percent - pre_percent));
        if (position_num == 0)
        {
            if (now_percent > basis_percent_after) {
                System.out.println("f开仓"+now_percent);
                position_num += 1;
            }else{
                System.out.println("不做处理"+now_percent);
            }
        }else if(position_num >= 1) {
            float flag_percent = now_percent - pre_percent;
            if (now_percent < basis_close_percent) {
                position_num = 0;
                System.out.println("清仓1+"+now_percent);
                System.out.println(position_num);
                System.out.println("============================");
                return;
            }
            System.out.println(flag_percent);
            if (Math.abs(flag_percent) > basis_percent_after) {
                if (flag_percent > 0)
                {
                    position_num += 1;
                    System.out.println(position_num);
                    System.out.println("++已经加仓"+now_percent);
                    System.out.println("============================");
                    return;
                }else if (flag_percent < 0) {
                    System.out.println("已经减仓"+now_percent);
                    position_num -= 1;
                    System.out.println(position_num);
                    System.out.println("============================");
                    return;
                }else {
                    System.out.println("--不做处理"+now_percent);
                    return;
                }
            }
            System.out.println("-不做处理"+now_percent);
        }
        System.out.println("开仓次数"+position_num);
        System.out.println("============================");

        //加仓
//        if (flag_percent > 0)
//        {
//            if (flag_percent > basis_percent_after) {
//                position_num += 1;
//                System.out.println("已经加仓");
//            }else{
//                System.out.println("不能加仓");
//            }
//        }else if (flag_percent < 0) {
//            if (Math.abs(flag_percent) > basis_percent_after) {
//                System.out.println("已经减仓");
//                position_num -= 1;
//            }else{
//                System.out.println("不能减仓");
//            }
//        }
    }
}
