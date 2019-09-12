package com.lyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.*;

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

    /**
     * 购物车
     * @param conn
     * @param session
     * @param item
     * @param count
     */
    public void addToCart(Jedis conn, String session, String item, int count) {
        if (count <= 0)
            conn.hdel("cart:" + session, item);
        else
            conn.hset("cart:" + session, item, String.valueOf(count));
    }

    /**
     * 定期清除session线程
     */
    public class CleanSessionThread extends Thread {
        private Jedis conn;
        private int limit;
        private boolean quit;

        public CleanSessionThread(int limit) {
            this.conn = new Jedis("127.0.0.1");
            this.conn.select(15);
            this.limit = limit;
        }

        public void quit() {
            quit = true;
        }

        @Override
        public void run() {
            while (!quit) {
                long size = conn.zcard("recent:");
                if (size <= limit) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                long endIndex = Math.min(size - limit, 100);
                Set<String> tokenSet = conn.zrange("recent:", 0, endIndex - 1);
                String[] tokens = tokenSet.toArray(new String[tokenSet.size()]);

                ArrayList<String> sessionKeys = new ArrayList<>();
                for (String token : tokens) {
                    sessionKeys.add("viewed:" + token);
                    //update:同时清除该用户的购物车
                    sessionKeys.add("cart:" + token);
                }

                conn.del(sessionKeys.toArray(new String[sessionKeys.size()]));
                conn.hdel("login:", tokens);
                conn.zrem("recent:", tokens);
            }
        }
    }
}
