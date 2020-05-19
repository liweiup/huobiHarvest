package com.contract.harvest.Aspects;

import com.contract.harvest.service.CacheService;
import com.contract.harvest.service.HuobiService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.aspectj.lang.reflect.MethodSignature;
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

    @Pointcut(
//            "execution(* com.contract.harvest.entity.*.*(..)) " +
            "execution(* com.contract.harvest.service.*.*(..)) " +
            "")
    public void pointCutVP(){};

    private static final long ONE_MINUTE = 10000;

    @AfterThrowing(value="pointCut()",throwing="exception")
    @ExceptionHandler
    public void logException(JoinPoint joinPoint,Exception exception){
        Object[] args = joinPoint.getArgs();
        String functionName = joinPoint.getSignature().getName();
        logger.error("方法"+functionName+"异常,异常信息:{"+exception+"}");
        exception.printStackTrace();
    }

    /**
     * 统计方法执行耗时Around环绕通知
     */
    @Around(value="pointCutVP()")
    public Object timeAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object obj = joinPoint.proceed(joinPoint.getArgs());
        long endTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取执行的方法名
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        // 打印耗时的信息
        this.printExecTime(methodName, startTime, endTime);
        return obj;
    }
    /**
     * 打印方法执行耗时的信息，如果超过了一定的时间，才打印
     * @param methodName
     * @param startTime
     * @param endTime
     */
    private void printExecTime(String methodName, long startTime, long endTime) {
        long diffTime = endTime - startTime;
        if (diffTime > ONE_MINUTE) {
            logger.warn(methodName + " 方法执行耗时：" + diffTime + "ms");
        }
    }
}
