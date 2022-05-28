package com.ludonghua.use.duplicate

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

import java.sql.Timestamp

/**
 * Author Luis
 * DATE 2022-05-27 21:13
 */
/**
 *  去重数据
 */
object Duplicate {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("Duplicate")
      .getOrCreate()

    import spark.implicits._

    val lines: DataFrame = spark.readStream
      .format("socket")
      .option("host", "hadoop102")
      .option("port", 9999)
      .load()

    val words: DataFrame = lines.as[String].map(line => {
      val arr: Array[String] = line.split(",")
      (arr(0), Timestamp.valueOf(arr(1)), arr(2))
    }).toDF("uid", "ts", "word")

    val wordCounts: Dataset[Row] = words
      .withWatermark("ts", "2 minutes")
      .dropDuplicates("uid")  // 去重重复数据 uid 相同就是重复.  可以传递多个列

    wordCounts.writeStream
      .outputMode("append")
      .format("console")
      .start
      .awaitTermination()
  }
}
