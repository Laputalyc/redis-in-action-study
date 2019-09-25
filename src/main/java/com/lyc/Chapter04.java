package com.lyc;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * Redis事务
 */
public class Chapter04 {
    /**
     * 购买商品
     * @param conn Jedis
     * @param buyerId 买家唯一表示id
     * @param sellerId 卖家唯一表示id
     * @param itemId 购买的商品
     * @return 是否购买成功
     */
    public boolean purchaseItem(Jedis conn, String buyerId, String sellerId, String itemId) {
        String seller = "user:" + sellerId;
        String buyer = "user:" + buyerId;
        String item = itemId + "." + sellerId;
        long end = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < end) {
            //对市场和卖家进行监控
            conn.watch("market:", buyer);
            //用户余额
            Double funds = Double.valueOf(conn.hget(buyer, "funds"));
            //商品价格
            Double price = conn.zscore("market:", item);
            if (Double.compare(funds, price) < 0) {
                conn.unwatch();
                return false;
            }
            //开启事务
            Transaction trans = conn.multi();
            /**
             * 1.从市场中移除该商品(商品数量减1)；
             * 2.买家背包中添加该商品；
             * 3.买家扣除商品价格；
             * 4.卖家余额增加商品价格
             */
            trans.srem("market:", item);
            trans.sadd("inventory:" + buyerId, itemId);
            trans.hincrByFloat(buyer, "funds", -price);
            trans.hincrByFloat(seller, "funds", price);
            List<Object> results = trans.exec();
            if (results == null) {
                continue;
            }
            return true;
        }
        return false;
    }
    /**
     * 商品上架
     * @param conn Jedis
     * @param itemId 出售者包裹中的商品Id
     * @param sellerId 出售者的唯一表示Id
     * @param price 商品的价格
     */
    public boolean listItem(Jedis conn, String itemId, String sellerId, double price) {
        //玩家包裹
        String inventory = "inventory:" + sellerId;
        //待出售商品信息
        String item = itemId + "." + sellerId;
        //进行5s钟的重试
        long end = System.currentTimeMillis() + 5000;
        /*在Jedis中的事务不用显示的使用pipelined，因为事务本身就是一次性提交的*/
        //Pipeline pipelined = conn.pipelined();
        while (System.currentTimeMillis() < end) {
            conn.watch(inventory);
            //判断用户包裹中是否存在该商品
            if (!conn.sismember(inventory, itemId)) {
                //如果用户包裹中不存在该商品，对当前连接进行重置
                conn.unwatch();
                return false;
            }
            /*开启事务*/
            Transaction trans = conn.multi();
            /*往市场中添加该商品*/
            trans.zadd("market:", price, item);
            /*从用户包裹中移除该商品*/
            trans.srem(inventory, itemId);
            List<Object> results = trans.exec();
            /*null response indicates that the transaction was aborted due to the watched key changing.*/
            if (results == null)
                continue;
            return true;
        }
        return false;
    }
}
