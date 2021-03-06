package com.ludonghua.realtime.utils

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

import java.util

/**
 * Author Luis
 * DATE 2022-05-28 20:31
 */
object RedisUtil {
  private val jedisPoolConfig: JedisPoolConfig = new JedisPoolConfig()
  jedisPoolConfig.setMaxTotal(100) //最大连接数
  jedisPoolConfig.setMaxIdle(20) //最大空闲
  jedisPoolConfig.setMinIdle(20) //最小空闲
  jedisPoolConfig.setBlockWhenExhausted(true) //忙碌时是否等待
  jedisPoolConfig.setMaxWaitMillis(500) //忙碌时等待时长 毫秒
  jedisPoolConfig.setTestOnBorrow(false) //每次获得连接的进行测试
  private val jedisPool: JedisPool = new JedisPool(jedisPoolConfig, "hadoop102", 6379)

  // 直接得到一个 Redis 的连接
  def getJedisClient: Jedis = {
    jedisPool.getResource
  }

  def main(args: Array[String]): Unit = {
    val client: Jedis = getJedisClient
    val strings: util.Set[String] = client.smembers("set1")
    println(strings)
  }
}

