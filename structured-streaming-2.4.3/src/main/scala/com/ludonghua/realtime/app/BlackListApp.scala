package com.ludonghua.realtime.app

import com.ludonghua.realtime.bean.AdsInfo
import com.ludonghua.realtime.utils.RedisUtil
import org.apache.spark.sql._
import org.apache.spark.sql.streaming.Trigger
import redis.clients.jedis.Jedis

/**
 * Author Luis
 * DATE 2022-05-28 20:32
 *
 * 统计黑名单
 * 实现实时的动态黑名单检测机制：
 * 将每天对某个广告点击超过阈值(比如:100次)的用户拉入黑名单。
 * 1. 黑名单应该是每天更新一次. 如果昨天进入黑名单, 今天应该重新再统计
 * 2. 把黑名单写入到 redis 中, 以供其他应用查看
 * 3. 已经进入黑名单的用户不再进行检测(提高效率)
 */
object BlackListApp {
  def statBlackList(spark: SparkSession, adsInfoDS: Dataset[AdsInfo]): Unit = {
    import spark.implicits._

    // 创建临时表: tb_ads_info
    adsInfoDS.createOrReplaceTempView("tb_ads_info")

    // 需求1: 黑名单 每天每用户每广告的点击量
    // 2.  按照每天每用户每id分组, 然后计数, 计数超过阈值(100)的查询出来
    val result: DataFrame = spark.sql(
      """
        |select
        | dayString,
        | userId
        |from tb_ads_info
        |group by dayString, userId, adsId
        |having count(1) >= 20
      """.stripMargin)

//    result.writeStream
//      .format("console")
//      .outputMode("complete")
//      .trigger(Trigger.ProcessingTime(1000))
//      .start()
//      .awaitTermination()

    // 3. 把点击量超过 100 的写入到redis中.
    result.writeStream
      .outputMode("update")
      .trigger(Trigger.ProcessingTime("2 seconds"))
      .foreach(new ForeachWriter[Row] {
        var client: Jedis = _

        override def open(partitionId: Long, epochId: Long): Boolean = {
          // 打开到redis的连接
          client = RedisUtil.getJedisClient
          client != null && client.isConnected
        }

        override def process(value: Row): Unit = {
          // 写入到redis  把每天的黑名单写入到set中  key: "day:blackList" value: 黑名单用户
          val dayString: String = value.getString(0)
          val userId: String = value.getString(1)
          client.sadd(s"day:blackList:$dayString", userId)
        }

        override def close(errorOrNull: Throwable): Unit = {
          // 关闭到redis的连接
          if (client != null) client.close()
        }
      })
      .option("checkpointLocation", "./blackList")
      .start()
      .awaitTermination()

  }
}
