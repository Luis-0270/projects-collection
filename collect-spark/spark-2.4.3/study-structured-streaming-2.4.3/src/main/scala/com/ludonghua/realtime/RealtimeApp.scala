package com.ludonghua.realtime

import com.ludonghua.realtime.app.{AdsClickCountApp, BlackListApp}
import com.ludonghua.realtime.bean.AdsInfo
import org.apache.spark.sql.{Dataset, SparkSession}

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Author Luis
 * DATE 2022-05-28 20:22
 */
object RealtimeApp {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[2]")
      .appName("RealtimeApp")
      .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._
    val dayStringFormatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
    val hmStringFormatter: SimpleDateFormat = new SimpleDateFormat("HH:mm")
    // 1. 从 kafka 读取数据, 为了方便后续处理, 封装数据到 AdsInfo 样例类中
    val adsInfoDS: Dataset[AdsInfo] = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
      .option("subscribe", "ads_log")
      .load
      .select("value")
      .as[String]
      .map(v => {
        val split: Array[String] = v.split(",")
        val date: Date = new Date(split(0).toLong)
        AdsInfo(split(0).toLong, new Timestamp(split(0).toLong), dayStringFormatter.format(date), hmStringFormatter.format(date), split(1), split(2), split(3), split(4))
      })

    //    adsInfoDS.writeStream
    //      .format("console")
    //      .outputMode("update")
    //      .option("truncate", "false")
    //      .start
    //      .awaitTermination()

    // 统计黑名单
//    BlackListApp.statBlackList(spark, adsInfoDS)

    // 广告点击量实时统计
    AdsClickCountApp.statAdsClickCount(spark, adsInfoDS)
  }
}
