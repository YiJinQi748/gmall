package com.atguigu.gmall.index.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    //自动续期
    private void renewExpiration(String lockName, String uuid, Integer expire){
        //1.判断自己的锁是否存在,存在则续期
        String script="if redis.call('hexists',KEYS[1],ARGV[1]) == 1" +
                "then " +
                "   return redis.call('expire',KEYS[1],ARGV[2]) " +
                "else " +
                "   return 0 " +
                "end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
                //如果续期成功则证明锁还在继续续期 不成功则不需要下一次续期
                if (flag){
                    renewExpiration(lockName,uuid,expire);
                }
            }
        },expire*1000/3);
    }
}
