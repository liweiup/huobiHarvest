package com.contract.harvest.service;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SubscriptionService {

    @Autowired
    private HuobiService huobiService;
    /**
     * 订阅分发
     * @param message
     */
    public void handleMessage(String message) {
        try {
            //处理订单
            switch (message) {
                case "handle_order":
                    huobiService.handleOrder();
                    break;
                case "handle_close_order":
                    huobiService.handleCloseOrder();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
