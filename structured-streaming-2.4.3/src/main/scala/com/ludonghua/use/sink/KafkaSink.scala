package com.ludonghua.use.sink

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-28 16:23
 */
object KafkaSink {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[1]")
      .appName("KafkaSink")
      .getOrCreate()

    import spark.implicits._

    val lines: DataFrame = spark.readStream
      .format("socket") // 设置数据源
      .option("host", "hadoop102")
      .option("port", 9999)
      .load

    val words: DataFrame = lines.as[String].flatMap(line => {
      line.split("\\W+")
    }).toDF("value")

    words.writeStream
      .outputMode("append")
      .format("kafka") //  // 支持 "orc", "json", "csv"
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop1043:9092")
      .option("topic", "test")
      .option("checkpointLocation", "./ck2") // 必须指定 checkpoint 目录
      .start
      .awaitTermination()
  }
}
