package com.lyc;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisSentinelTest {
    //private static final Logger logger = LoggerFactory.getLogger(RedisSentinelTest.class);
    public static void main(String[] args) {
        String masterName = "mymaster";
        Set<String> sentinels = new HashSet<>();
        sentinels.add("127.0.0.1:26379");
        sentinels.add("127.0.0.1:26380");
        sentinels.add("127.0.0.1:26381");
        while (true) {
            JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(masterName, sentinels);
            Jedis jedis = null;
            try  {
                jedis = jedisSentinelPool.getResource();
                int index = new Random().nextInt();
                String key = "K:" + index;
                String value = "V:" + index;
                jedis.set(key, value);
                //logger.info("{} value is {}", key, value);
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (Exception e) {
                //logger.error(e.getMessage(), e);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }
}
