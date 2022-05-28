package com.ludonghua.use.source

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-26 21:20
 */
/**
 * 批处理kafka数据
 */
object KafkaSourceRead {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("KafkaSourceRead")
      .getOrCreate()

    import spark.implicits._

    val df: DataFrame = spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
      .option("subscribe", "topic1")
      .option("startingOffsets", """{"topic1":{"0":12}}""")
      .option("endingOffsets", "latest")
      .load
      //            .select("value", "key")
      .selectExpr("cast(value as string)")
      .as[String]
      .flatMap(_.split(" "))
      .groupBy("value")
      .count()

    // 批处理的方式, 只需要执行一次
    df.write
      .format("console")
      .option("truncate", value = false)
      .save()
  }
}