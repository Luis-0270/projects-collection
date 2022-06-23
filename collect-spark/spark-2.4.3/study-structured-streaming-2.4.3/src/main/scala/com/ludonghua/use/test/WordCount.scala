package com.ludonghua.use.test

import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-26 19:44
 */
object WordCount {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .appName("WordCount")
      .getOrCreate()

    import spark.implicits._

    // 1. 从载数据数据源加
    val lines: DataFrame = spark.readStream
      .format("socket")
      .option("host", "hadoop102")
      .option("port", 9999)
      .load

    //    val wordCount = lines.as[String].flatMap(_.split(" ")).groupBy("value").count()

    lines.as[String].flatMap(_.split(" ")).createOrReplaceTempView("w")
    val wordCount: DataFrame = spark.sql(
      """
        |select
        | *,
        | count(*) as cnt
        |from w
        |group by value
      """.stripMargin)

    // 2. 输出
    val result: StreamingQuery = wordCount.writeStream
      .format("console")
      .outputMode("update") // complete append update
      .trigger(Trigger.ProcessingTime("2 seconds"))
      .start

    result.awaitTermination()
    spark.stop()
  }
}
