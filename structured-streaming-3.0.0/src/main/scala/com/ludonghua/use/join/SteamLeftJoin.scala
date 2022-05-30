package com.ludonghua.use.join

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.OutputMode

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-29 19:47
 */
object SteamLeftJoin {
  case class ImpressionData(impressionAdId: Int, data: String, impressionTime: Timestamp)

  case class ClickData(clickId: Int, data: String, clickTime: Timestamp)

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setMaster("local[*]")
      .set("spark.sql.shuffle.partitions", "1")
    val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    val line1: DataFrame = sparkSession.readStream.format("socket")
      .option("host", "hadoop102").option("port", "7777").load()
    val line2: DataFrame = sparkSession.readStream.format("socket")
      .option("host", "hadoop102").option("port", "7778").load()
    import sparkSession.implicits._
    val impressionStream: Dataset[ImpressionData] = line1.as[String].map(item => {
      val data: Array[String] = item.split(",")
      ImpressionData(data(0).toInt, data(1), Timestamp.valueOf(data(2)))
    }).withWatermark("impressionTime", "2 hours")

    val clickStream: Dataset[ClickData] = line2.as[String].map(item => {
      val data: Array[String] = item.split(",")
      ClickData(data(0).toInt, data(1), Timestamp.valueOf(data(2)))
    }).withWatermark("clickTime", "3 hours")
    import org.apache.spark.sql.functions._
    val result: DataFrame = clickStream.join(impressionStream, expr(
      """
        |impressionAdId=clickId
        |and clickTime >= impressionTime
        |and clickTime <= impressionTime + interval 1 hour
        |""".stripMargin), "left")
    result.writeStream.outputMode(OutputMode.Append()).format("console")
      .option("truncate", "false").start().awaitTermination()
  }
}
