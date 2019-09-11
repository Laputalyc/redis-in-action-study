package com.lyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

/**
 * 文章评分功能实现
 */
public class Chapter01 {
    private static final Logger logger = LoggerFactory.getLogger(Chapter01.class);
    private static final int ONE_WEEK_IN_SENONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new Chapter01().run();
    }
    public void run() {
        Jedis conn = new Jedis("127.0.0.1", 6379);
        //选择数据库redis默认0~15号数据库
        conn.select(15);
        String articleId = postArticle(conn, "usrname", "A title", "http://www.baidu.com");
        System.out.println("We posted a new article with id: " + articleId);
        System.out.println("Its HASH looks like:");
        Map<String,String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry<String,String> entry : articleData.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * 对文章进行投票
     * @param conn Jedis
     * @param user 投票用户
     * @param article 投票文章
     */
    public void articleVote(Jedis conn, String user, String article) {
        long cutoff = (System.currentTimeMillis()/1000) - ONE_WEEK_IN_SENONDS;
        if (conn.zscore("time:", article) < cutoff) {
            logger.info("投票时间已截止！" );
            return;
        }
        /**
         * 如果该用户没有投过票：
         * 1.在该文章的已投票用户集合中增加当前用户；
         * 2.为该文章增加投票评分
         * 3.为该文章的投票数加1
         */
        String articleId = article.substring(article.indexOf(':') + 1);
        logger.info("投票文章的ID为{}", articleId);
        if (conn.sadd("voted:" + articleId, user) == 1) {
            conn.zincrby("score", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1);
        }
        logger.info("用户{}已为文章{}投过票！", user, articleId);
    }

    /**
     * 发布新文章
     * @param conn Jedis
     * @param user 发布者
     * @param title 标题
     * @param link 文章链接
     * @return 新发布文章的ID
     */
    public String postArticle(Jedis conn, String user, String title, String link) {
        //采用自增ID作为新文章的ID
        String articleId = String.valueOf(conn.incr("article:"));

        /**
         * 为新发布的文章创建已投票用户集合
         * 将自己加入到已投票集合中
         * 同时为该集合设置过期时间
         */
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SENONDS);

        long now = System.currentTimeMillis() / 1000;
        //采用散列类型为新发布的文章创建详细信息
        String article = "article:" + articleId;
        HashMap<String, String> articleData = new HashMap<>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);

        //创建按照时间和分数排序的有序列表
        conn.zadd("score", now + VOTE_SCORE, article);
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * 取出评分最高或发布时间最新的文章
     * @param conn Jedis
     * @param page 页码
     * @param order 评价标准(time:或者score:)
     * @return order标准下，文章评分由高到低或者发布时间又新到旧的第page页文章
     */
    public List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        //计算需要获取的文章范围
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE;
        logger.info("获取的文章是从{}到{}", start, end);

        //逆序取出文章的ID
        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }

    /**
     * 默认使用评分作为评价标准
     * @param conn
     * @param page
     * @return
     */
    public List<Map<String, String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    /**
     * 为文章添加分组
     * @param conn Jedis
     * @param articleId 分组文章ID
     * @param toAdd 文章分组标签
     */
    public void addGroups(Jedis conn, String articleId, String[] toAdd) {
        String article = "article:" + articleId;
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);
        }
    }

    /**
     * 为文章删除分组
     * @param conn Jedis
     * @param articleId 文章ID
     * @param toRem 待移除标签
     */
    public void remGroups(Jedis conn, String articleId, String[] toRem) {
        String article = "article:" + articleId;
        for (String group : toRem) {
            conn.srem("group:" + group, article);
        }
    }

    /**
     * 分组获取最高评价(发布时间，分数)指定页面的文章
     * @param conn Jedis
     * @param group 分组标签
     * @param page 页面
     * @param order 评价标准（发布时间，评分）
     * @return
     */
    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        String key = order + group;
        if (!conn.exists(key)) {
            logger.info("内存中没有缓存key的记录。");
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            //设置缓存时间为60s
            conn.expire(key, 60);
        }
        logger.info("内存中已缓存{}", key);
        return getArticles(conn, page, key);
    }

    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }
}
