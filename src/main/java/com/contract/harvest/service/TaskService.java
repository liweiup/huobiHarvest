package com.contract.harvest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service

public class TaskService  {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private HuobiService huobiService;

    @Qualifier("harvestExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Async
    public void exec_invoke_bi(Map<String,String> Params,Integer num) {
        taskExecutor.execute(() -> {
            synchronized(getBuildLock(Params.get("symbol"))) {
                try {
                    huobiService.invoke_bi(Params);
                } catch (InterruptedException e) {
                    logger.error("线程异常"+e.getMessage());
                } catch (Exception e) {
                    logger.error("线程外异常"+e.getMessage());
                }
            }
        });
    }
    /**
     * 获取锁
     */
    private String getBuildLock(String lockStr) {
        lockStr = lockStr.intern();
        return lockStr;
    }
}