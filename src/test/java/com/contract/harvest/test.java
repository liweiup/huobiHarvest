package com.contract.harvest;

import com.contract.harvest.config.HuobiConfig;
import com.contract.harvest.config.RedisConfig;
import com.contract.harvest.service.*;
import org.junit.Test;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class test {

    @Autowired
    private HuobiService huobiService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CacheService cacheService;
    @Autowired
    private VerifyParams verifyParams;

    @Autowired
    private ElesService elesService;

    private static final Logger logger = LoggerFactory.getLogger(HuobiService.class);

    @Test
    public void TestLoadingCache() throws Exception{
        Map<String,String> params = new HashMap<>();
//        params.put("symbol","BSV");
        params.put("symbol","BSV");
//        params.put("symbol","BTC");
//        huobiService.invoke_bi(params);
//        huobiService.handleOrder();
//        huobiService.handleCloseOrder();
//        logger.error("222");
//        float a = (float) 188.452;
//        float b = (float) 188.428;

//        System.out.println(BigDecimal.valueOf((a - b) / b));
//        System.out.println(BigDecimal.valueOf(a));
//        System.out.println(verifyParams.getFloatNum((a - b) / b));
        elesService.get_harvest_log("");
    }


    @Autowired
    private TaskService taskService;


    @Qualifier("harvestExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Test
    public void testVoid() throws Exception {
        Integer num = 1;
        while (true) {
            Map<String,String> p = new HashMap<>();
            Thread.sleep(2000);
            taskService.exec_invoke_bi(p,num);
//            taskExecutor.execute(taskService,num);
            System.out.println("线程池中线程数目："+taskExecutor.getThreadPoolExecutor().getPoolSize()+"，队列中等待执行的任务数目："+
                    taskExecutor.getThreadPoolExecutor().getQueue().size()+"，已执行玩别的任务数目："+taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
            num++;
        }
    }


}
