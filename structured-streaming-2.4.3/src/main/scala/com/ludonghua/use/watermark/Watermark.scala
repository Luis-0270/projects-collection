package com.ludonghua.use.watermark

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-27 16:03
 */
object Watermark {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("Watermark1")
      .getOrCreate()

    import spark.implicits._

    val lines: DataFrame = spark.readStream
      .format("socket")
      .option("host", "hadoop102")
      .option("port", 9999)
      .load

    // 输入的数据中包含时间戳, 而不是自动添加的时间戳
    // 2019-09-25 10:20:00,hello hello hello
    val words: DataFrame = lines.as[String]
      .flatMap(line => {
        val split: Array[String] = line.split(",")
        split(1).split(" ").map((_, Timestamp.valueOf(split(0))))
      })
      .toDF("word", "timestamp")

    import org.apache.spark.sql.functions._


    val wordCounts: Dataset[Row] = words
      // 添加watermark, 参数 1: event-time 所在列的列名 参数 2: 延迟时间的上限.
      .withWatermark("timestamp", "2 minutes")
      .groupBy(
        window($"timestamp", "10 minutes", "2 minutes"),
        $"word")
      .count()


    val query: StreamingQuery = wordCounts.writeStream
      .outputMode("append")
      .trigger(Trigger.ProcessingTime(2000))
      .format("console")
      .option("truncate", "false")
      .start
    query.awaitTermination()
  }
}
