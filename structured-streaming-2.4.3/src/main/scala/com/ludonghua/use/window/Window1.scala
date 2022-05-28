package com.ludonghua.use.window

import org.apache.spark.sql.SparkSession

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-27 00:17
 */
object Window1 {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("window1")
      .getOrCreate()
    import spark.implicits._

    // 导入spark提供的全局的函数
    import org.apache.spark.sql.functions._
    val lines = spark.readStream
      .format("socket") // 设置数据源
      .option("host", "hadoop102")
      .option("port", 9999)
      .option("includeTimestamp", value = true) // 给产生的数据自动添加时间戳
      .load
      .as[(String, Timestamp)]
      .flatMap {
        case (words, ts) => words.split("\\W+").map((_, ts))
      }
      .toDF("word", "ts")
      .groupBy(
        window($"ts", "4 minutes", "2 minutes"),
        $"word")
      .count()
    lines.writeStream
      .format("console")
      .outputMode("update")
      .option("truncate", value = false)
      .start()
      .awaitTermination()
  }
}
