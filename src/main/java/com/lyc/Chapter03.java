package com.lyc;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;

import java.util.HashMap;
import java.util.Set;

/**
 * Redis命令
 */
public class Chapter03 {
    public static void main(String[] args) {
        Jedis conn = new Jedis("localhost", 6379);

        HashMap<String, Double> map1 = new HashMap<>();
        HashMap<String, Double> map2 = new HashMap<>();
        map1.put("a", 1.0); map1.put("b", 2.0); map1.put("c", 3.0);
        map2.put("b", 4.0); map2.put("c", 1.0); map2.put("d", 0.0);
        conn.zadd("zset-1", map1);
        conn.zadd("zset-2", map2);
        ZParams zParams = new ZParams();
        /**
         * 乘法因子，是分别对每个集合进行相乘之后再进行集合间的操作
         */
        conn.zinterstore("zset-i", zParams.weightsByDouble(0.5, 3.0).aggregate(ZParams.Aggregate.MIN), "zset-1", "zset-2");
        Set<Tuple> tuples = conn.zrangeWithScores("zset-i", 0, -1);
        System.out.println(tuples);

    }
}
