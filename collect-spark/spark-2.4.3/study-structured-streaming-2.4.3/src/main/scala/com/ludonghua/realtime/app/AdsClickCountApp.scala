package com.ludonghua.realtime.app

import com.ludonghua.realtime.bean.AdsInfo
import com.ludonghua.realtime.utils.RedisUtil
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import redis.clients.jedis.Jedis

/**
 * Author Luis
 * DATE 2022-05-28 22:53
 *
 * 每天每地区每城市广告点击量实时统计
 */
object AdsClickCountApp {
  val key: String = "date:area:city:ads"

  def statAdsClickCount(spark: SparkSession, filteredAdsInfoDS: Dataset[AdsInfo]): Unit = {
    val resultDF: DataFrame = filteredAdsInfoDS
      .groupBy("dayString", "area", "city", "adsId")
      .count()

    resultDF.writeStream
      .outputMode("update")
      .trigger(Trigger.ProcessingTime("2 seconds"))
      .foreachBatch((df, batchId) => { // 使用foreachBatch
        if (df.count() > 0) {
          df.cache() // 做缓存防止重复调用
          df.foreachPartition(rowIt => {
            val client: Jedis = RedisUtil.getJedisClient
            // 1. 把数据存入到map中, 向redis写入的时候比较方便
            val fieldValueMap: Map[String, String] = rowIt.map(row => {
              // 2019-08-19:华北:北京:5
              val field: String = s"${row.getString(0)}:${row.getString(1)}:${row.getString(2)}:${row.getString(3)}"
              val value: Long = row.getLong(4)
              (field, value.toString)
            }).toMap
            // 2. 写入到redis
            // 用于把scala的集合转换成java的集合
            import scala.collection.JavaConversions._
            if (fieldValueMap.nonEmpty) client.hmset(key, fieldValueMap)
            client.close()
          })

          df.unpersist() // 释放缓存
        }
      })
      .start
      .awaitTermination
  }
}
