package com.contract.harvest.Aspects;

import com.contract.harvest.service.CacheService;
import com.contract.harvest.service.HuobiService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Aspect
public class HuobiServiceAspects {

    private static final Logger logger = LoggerFactory.getLogger(HuobiServiceAspects.class);

    @Pointcut("execution(public void com.contract.harvest.service.HuobiService.*(..))")
    public void pointCut(){};

    @AfterThrowing(value="pointCut()",throwing="exception")
    @ExceptionHandler
    public void logException(JoinPoint joinPoint,Exception exception){
        Object[] args = joinPoint.getArgs();
        String functionName = joinPoint.getSignature().getName();
        logger.info("方法"+functionName+"异常,异常信息:{"+exception+"}");
    }
}
