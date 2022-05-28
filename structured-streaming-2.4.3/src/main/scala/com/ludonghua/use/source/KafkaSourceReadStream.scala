package com.ludonghua.use.source

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-26 20:53
 */
/**
 *  流处理kafka数据
 */
object KafkaSourceReadStream {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("KafkaSourceReadStream")
      .getOrCreate()

    import spark.implicits._

    val df: DataFrame = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092")
      .option("subscribe", "topic1")
      .load
      //            .select("value", "key")
      .selectExpr("cast(value as string)")
      .as[String]
      .flatMap(_.split(" "))
      .groupBy("value")
      .count()

    df.writeStream
      .format("console")
      .outputMode("update")
      .option("truncate", value = false)
      .start
      .awaitTermination()
  }
}
