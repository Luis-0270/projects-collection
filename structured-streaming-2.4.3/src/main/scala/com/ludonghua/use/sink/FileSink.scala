package com.ludonghua.use.sink

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Author Luis
 * DATE 2022-05-28 10:41
 */
object FileSink {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[1]")
      .appName("FileSink")
      .getOrCreate()
    import spark.implicits._

    val lines: DataFrame = spark.readStream
      .format("socket") // 设置数据源
      .option("host", "hadoop102")
      .option("port", 9999)
      .load

    val words: DataFrame = lines.as[String].flatMap(line => {
      line.split("\\W+").map(word => {
        (word, word.reverse)
      })
    }).toDF("原单词", "反转单词")

    words.writeStream
      .outputMode("append")
      .format("json") //  // 支持 "orc", "json", "csv"
      .option("path", "./filesink") // 输出目录
      .option("checkpointLocation", "./ck1")  // 必须指定 checkpoint 目录
      .start
      .awaitTermination()
  }
}
