package com.lyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * 使用Redis构建web应用
 */
public class Chapter02 {
    private static final Logger logger = LoggerFactory.getLogger(Chapter02.class);

    /**
     * 尝试获取并返回令牌对应的用户
     * @param conn Jedis
     * @param token 令牌
     */
    public String checkToken(Jedis conn, String token) {
        return conn.hget("login:", token);
    }

    /**
     * 避免内存占用过多，定时清理会话
     * @param conn Jedis
     */
    public void cleanSession(Jedis conn) {

    }

    /**
     * 更新令牌：
     * 1.将token---user存储到散列'login:'里面;
     * 2.将token---timestamp存储到有序集合'recent:'里面;
     * 3.如果该用户浏览过商品（即item != null),则将item存放到该用户浏览的有序集合'viewd:token'中，始终保持最新浏览的25条记录
     * @param conn Jedis
     * @param token 令牌
     * @param user 用户
     * @param item 用户浏览过的商品
     */
    public void updateToken(Jedis conn, String token, String user, String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        conn.hset("login:", token, user);
        conn.zadd("recent:", timestamp, token);
        if (item != null) {
            logger.info("用户{}浏览过商品{}", user, item);
            conn.zadd("viewed:" + token, timestamp, item);
            //最近浏览中只保留最新的25条浏览记录
            conn.zremrangeByRank("viewed:" + token, 0, -26);
            conn.zincrby("viewed:", -1, item);
        }
    }

}
