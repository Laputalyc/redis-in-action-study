package com.lyc;

/**
 * Redis命令
 */
public class Chapter03 {
    public static void main(String[] args) {
/*        Jedis conn = new Jedis("localhost", 6379);
        for (int i = 0; i < 3000000; i++) {
            conn.set("string:" + i, String.valueOf(i));
            conn.sadd("set:" + i, String.valueOf(i));
        }*/




/*        HashMap<String, Double> map1 = new HashMap<>();
        HashMap<String, Double> map2 = new HashMap<>();
        map1.put("a", 1.0); map1.put("b", 2.0); map1.put("c", 3.0);
        map2.put("b", 4.0); map2.put("c", 1.0); map2.put("d", 0.0);
        conn.zadd("zset-1", map1);
        conn.zadd("zset-2", map2);
        ZParams zParams = new ZParams();
        *//**
         * 乘法因子，是分别对每个集合进行相乘之后再进行集合间的操作
         *//*
        conn.zinterstore("zset-i", zParams.weightsByDouble(0.5, 3.0).aggregate(ZParams.Aggregate.MIN), "zset-1", "zset-2");
        Set<Tuple> tuples = conn.zrangeWithScores("zset-i", 0, -1);
        System.out.println(tuples);*/

/*        conn.rpush("sort-input", "23", "15", "110", "7");
        //自然序
        System.out.println(conn.lrange("sort-input", 0, -1));
        //根据数字大小对元素进行排序
        System.out.println(conn.sort("sort-input"));
        //根据字母表大小对元素进行排序
        SortingParams sortingParams = new SortingParams();
        List<String> sorted = conn.sort("sort-input", sortingParams.alpha());
        System.out.println(conn.lrange("sort-input", 0, -1));
        System.out.println(sorted);*/
        //conn.rpush("sort-input", "aa", "aA", "bb", "bB", "cc", "cd");
        //按照字典序进行排序---这一句执行之后dbsize = 1
        //System.out.println("按照字典序进行排序：" + conn.sort("sort-input", new SortingParams().alpha()));
        //System.out.println("按照字典序进行排序:" + conn.sort("sort-input", new SortingParams().alpha(), "new-sort-input"));
    }
}
