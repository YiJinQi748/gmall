package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:category:";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> responseVo = this.gmallPmsClient.queryCategoryByPid(0L);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        return categoryEntities;
    }

    public List<CategoryEntity> queryLv23CategoriesByPid(Long pid) {
        //先查询缓存,如果缓存可以命中则直接返回
        String json = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }
        //添加分布式锁,防止缓存击穿
        RLock fairLock = this.redissonClient.getFairLock("index:category:lock" + pid);
        fairLock.lock();
        try {
            //再次查询缓存如果命中则直接返回,请求获取锁的过程中可能有其他请求已经把数据放入了缓存中
            String json2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json2)) {
                return JSON.parseArray(json2, CategoryEntity.class);
            }
            //如果缓存无法命中,则查询数据或者远程调用获取数据放入缓存
            ResponseVo<List<CategoryEntity>> responseVo = this.gmallPmsClient.queryLV23CategoriesByPid(pid);
            //二级分类和三级分类集合数据
            List<CategoryEntity> categoryEntities = responseVo.getData();
            //放入缓存
            if (CollectionUtils.isEmpty(categoryEntities)) {
                //缓存null数据过期时间为5分钟 防止缓存穿透
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            } else {
                //为了防止缓存雪崩,给缓存时间添加随机值
                int randomTime = new Random().nextInt(10);
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 90 + randomTime, TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {
            fairLock.unlock();
        }
    }


    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        try {
            String number = this.stringRedisTemplate.opsForValue().get("number");
            if (StringUtils.isBlank(number)) {
                this.stringRedisTemplate.opsForValue().set("number", "1");
            }
            int num = Integer.parseInt(number);
            this.stringRedisTemplate.opsForValue().set("number", String.valueOf(++num));
        } finally {
            lock.unlock();
        }
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);
    }
}
