package com.ludonghua.use.sink

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-28 16:29
 */
object KafkaSinkBatch {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[1]")
      .appName("KafkaSinkBatch")
      .getOrCreate()
    import spark.implicits._

    val wordCount: DataFrame = spark.sparkContext.parallelize(Array("hello hello abc", "abc hello")).flatMap(_.split(" "))
      .toDF("word")
      .groupBy("word")
      .count()
      .map(row => row.getString(0) + "," + row.getLong(1))  // hello,10
      .toDF("value")  // 写入数据时候, 必须有一列 "value"
    println(wordCount.isStreaming)
    wordCount.write  // batch 方式
      .format("kafka")
      .option("kafka.bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092") // kafka 配置
      .option("topic", "test1") // kafka 主题
      .save()
  }
}
