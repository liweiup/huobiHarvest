package com.contract.harvest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 默认过期时长，单位：秒
     */
    public static final long DEFAULT_EXPIRE = 60 * 60 * 24;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ListOperations<String, Object> listOperations;

    @Resource
    private SetOperations<String, Object> setOperations;

    /**
     * 不设置过期时长
     */
    public static final long NOT_EXPIRE = -1;

    /**
     * 获取一个key的值
     */
    public String getValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置一个key的值
     */
    public void setValue(String key,String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 右边插入
     */
    public Long rightPush(String key,String value) {
        return listOperations.rightPush(key,value);
    }

    /**
     * 将一个值放入列表头部
     */
    public void lpush(String key, String value) {
        listOperations.leftPush(key,value);
    }
    /**
     * 左边取出
     */
    public Object leftPop(String key) {
        return listOperations.leftPop(key);
    }
    /**
     * 右边取出
     */
    public Object rightPop(String key) {
        return listOperations.rightPop(key);
    }
    /**
     * 获取一个list的长度
     */
    public Long getListLen(String key) {
        return listOperations.size(key);
    }

    /**
     * 索引获取列表中的元素
     */
    public String getLindex(String key,Long index) {
        return String.valueOf(listOperations.index(key,index));
    }
    /**
     * 修剪列表
     */
    public void listTrim(String key,long start, long end) {
        listOperations.trim(key,start,end);
    }
    /**
     * 按区间获取列表值
     */
    public List<Object> lrangeList(String key, long start, long end){
        return listOperations.range(key,start,end);
    }
    /**
     * add一个set值
     * @return
     */
    public Long addSet(String key, String value) {
        return setOperations.add(key,value);
    }

    /**
     * 获取所有的set值
     */
    public Set<Object> getSetMembers(String key) {
        return setOperations.members(key);
    }
    /**
     * 订阅通知
     * @param channel_flag 频道
     * @param Content 通知内容
     */
    public void convertAndSend(String channel_flag,String Content) {
        stringRedisTemplate.convertAndSend(channel_flag,Content);
    }

    public boolean existsKey(String key) {

        return redisTemplate.hasKey(key);
    }

    /**
     * 重名名key，如果newKey已经存在，则newKey的原值被覆盖
     *
     * @param oldKey
     * @param newKey
     */
    public void renameKey(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * newKey不存在时才重命名
     *
     * @param oldKey
     * @param newKey
     * @return 修改成功返回true
     */
    public boolean renameKeyNotExist(String oldKey, String newKey) {
        return redisTemplate.renameIfAbsent(oldKey, newKey);
    }

    /**
     * 删除key
     *
     * @param key
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 删除多个key
     *
     * @param keys
     */
    public void deleteKey(String... keys) {
        Set<String> kSet = Stream.of(keys).map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 删除Key的集合
     *
     * @param keys
     */
    public void deleteKey(Collection<String> keys) {
        Set<String> kSet = keys.stream().map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 设置key的生命周期
     *
     * @param key
     * @param time
     * @param timeUnit
     */
    public void expireKey(String key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

    /**
     * 指定key在指定的日期过期
     *
     * @param key
     * @param date
     */
    public void expireKeyAt(String key, Date date) {
        redisTemplate.expireAt(key, date);
    }

    /**
     * 查询key的生命周期
     *
     * @param key
     * @param timeUnit
     * @return
     */
    public long getKeyExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * 将key设置为永久有效
     *
     * @param key
     */
    public void persistKey(String key) {
        redisTemplate.persist(key);
    }
}